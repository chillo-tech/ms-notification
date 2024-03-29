on:
  push:
    branches:
      #- develop
      - feature/*
  workflow_dispatch:

env:
  NODE_VERSION: '14.x'
  APPLICATION_NAME: 'notifications'
  APPLICATION_TYPE: 'ms'

permissions:
  contents: read

jobs:
  create-folder:
    name: Create folder
    runs-on: ubuntu-latest
    steps:
      - name: params
        run: echo ${{ github.ref_name }} ${{ github.sha }}
      - name: Create folder
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.RECETTE_APPLICATIONS_HOST }}
          port: ${{ secrets.RECETTE_APPLICATIONS_PORT }}
          username: ${{ secrets.RECETTE_APPLICATIONS_USERNAME }}
          password: ${{ secrets.RECETTE_APPLICATIONS_PASSWPRD }}
          key: ${{ secrets.RECETTE_APPLICATIONS_SSH_PRIVATE_KEY }}
          script: |
            sudo mkdir -p /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}
            sudo chmod ugo+rwx /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}

  copy-configs:
    name: 'copy configs'
    runs-on: ubuntu-latest
    needs: create-folder
    steps:
      - uses: actions/checkout@master
      - name: update configs
        run: |
          sed -i 's|IMAGE_NAME|simachille/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}:${{ github.sha }}|' docker-compose.yml
          sed -i 's|APP_JAR|simachille/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}-${{ github.sha }}.jar|' Dockerfile

      - name: copy file via ssh password
        uses: appleboy/scp-action@master
        with:
          host: ${{ secrets.RECETTE_APPLICATIONS_HOST }}
          port: ${{ secrets.RECETTE_APPLICATIONS_PORT }}
          username: ${{ secrets.RECETTE_APPLICATIONS_USERNAME }}
          password: ${{ secrets.RECETTE_APPLICATIONS_PASSWPRD }}
          key: ${{ secrets.RECETTE_APPLICATIONS_SSH_PRIVATE_KEY }}
          source: 'docker-compose.yml'
          target: '/opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}'
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v3

      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'adopt'

      - name: Build with Maven
        run: mvn -Dmaven.test.skip=true clean compile package

      - name: rename
        run: |
          mv ./target/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}-0.0.1-SNAPSHOT.jar ./target/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}-${{ github.sha }}.jar
          ls -al target

      - name: Upload jar
        uses: actions/upload-artifact@v3
        with:
          name: ${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}
          path: ./target/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}-${{ github.sha }}.jar

  build-to-nexus:
    name: 'Build to nexus'
    runs-on: ubuntu-latest
    needs: build
    steps:
      - name: Download artifact
        uses: actions/download-artifact@v3
        with:
          name: ${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}
      - name: Check
        run: |
          ls -al

    #  - name: Nexus Repo Publish
    #    uses: sonatype-nexus-community/nexus-repo-github-action@master
    #    with:
    #     serverUrl: ${{ secrets.NEXUS_HOSTNAME }}
    #     username: ${{ secrets.NEXUS_USERNAME }}
    #    password: ${{ secrets.NEXUS_PASSWORD }}
    #    format: maven2
    #    repository: maven-releases
    #   coordinates: groupId=com.cs artifactId=${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}} version=${{ github.sha }}
    #   assets: extension=jar
    #   filename: ${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}-${{ github.sha }}.jar

  dockerise-and-push-to-nexus:
    name: 'dockerise-and-push-to-nexus'
    runs-on: ubuntu-latest
    needs: build
    steps:
      - name: Checkout repository
        uses: actions/checkout@v3
      - name: Download artifact
        uses: actions/download-artifact@v3
        with:
          name: ${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}
      - name: Check
        run: |
          ls -al

      - name: rename
        run: |
          mv ${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}-*.jar ${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}.jar
          ls -al

      - name: update configs
        run: |
          cat Dockerfile
          sed -i 's|APP_JAR|simachille/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}:${{ github.sha }}.jar|' Dockerfile

      - name: Set up QEMU
        uses: docker/setup-qemu-action@v2

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2

      - name: Login to DockerHub
        uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Build and push
        uses: docker/build-push-action@v3
        with:
          context: .
          push: true
          tags: simachille/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}:${{ github.sha }}

  run-container:
    name: 'Run container'
    runs-on: ubuntu-latest
    needs: [dockerise-and-push-to-nexus]
    steps:
      - name: Run container
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.RECETTE_APPLICATIONS_HOST }}
          port: ${{ secrets.RECETTE_APPLICATIONS_PORT }}
          username: ${{ secrets.RECETTE_APPLICATIONS_USERNAME }}
          password: ${{ secrets.RECETTE_APPLICATIONS_PASSWPRD }}
          key: ${{ secrets.RECETTE_APPLICATIONS_SSH_PRIVATE_KEY }}
          script: |
              docker compose -f /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/docker-compose.yml stop
              docker compose -f /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/docker-compose.yml rm -f
              docker rmi -f simachille/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}:${{ github.sha }}

              rm -f /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

              echo SENDINBLUE_TOKEN=${{ secrets.SENDINBLUE_TOKEN }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
              echo WHATSAPP_WABA_ID=${{ secrets.WHATSAPP_WABA_ID }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
              echo WHATSAPP_PHONE=${{ secrets.WHATSAPP_PHONE }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
              echo WHATSAPP_ACCOUNT_ID=${{ secrets.WHATSAPP_ACCOUNT_ID }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
              echo WHATSAPP_TOKEN=${{ secrets.WHATSAPP_TOKEN }}  >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
              echo TWILIO_ACCOUNT_ID=${{ secrets.TWILIO_ACCOUNT_ID }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
              echo TWILIO_ACCOUNT_SECRET=${{ secrets.TWILIO_ACCOUNT_SECRET }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
              echo TWILIO_SERVICE_ID=${{ secrets.TWILIO_SERVICE_ID }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
              echo VONAGE_API_KEY=${{ secrets.VONAGE_API_KEY }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
              echo VONAGE_API_SECRET=${{ secrets.VONAGE_API_SECRET }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

              echo RABBITMQ_IP=${{ secrets.RABBITMQ_IP }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
              echo RABBITMQ_PORT=${{ secrets.RABBITMQ_PORT }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
              echo RABBITMQ_USERNAME=${{ secrets.RABBITMQ_USERNAME }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env
              echo RABBITMQ_PASSWORD=${{ secrets.RABBITMQ_PASSWORD }} >> /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

              docker compose -f /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/docker-compose.yml pull
              docker compose -f /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/docker-compose.yml up -d

              rm -f /opt/applications/${{env.APPLICATION_NAME}}/${{env.APPLICATION_TYPE}}-${{env.APPLICATION_NAME}}/.env

