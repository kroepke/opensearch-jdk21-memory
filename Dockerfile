FROM opensearchproject/opensearch:2.12.0
USER root
RUN dnf install -y java-17-amazon-corretto
USER opensearch
ENV JAVA_HOME=/usr
