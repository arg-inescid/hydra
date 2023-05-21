FROM debian
RUN apt-get update
RUN apt-get install -y build-essential
RUN apt-get install -y zlib1g-dev
RUN apt-get install -y sudo
COPY disk/ /
ENV JAVA_HOME=/jvm
ENV ARGO_HOME=/argo
