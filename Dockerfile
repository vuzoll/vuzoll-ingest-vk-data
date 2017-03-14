FROM openjdk:8

RUN mkdir -p /usr/src/vuzoll-ingest-vk-data && mkdir -p /usr/app

COPY build/distributions/* /usr/src/vuzoll-ingest-vk-data/

RUN unzip /usr/src/vuzoll-ingest-vk-data/vuzoll-ingest-vk-data-*.zip -d /usr/app/ && ln -s /usr/app/vuzoll-ingest-vk-data-* /usr/app/vuzoll-ingest-vk-data

WORKDIR /usr/app/vuzoll-ingest-vk-data

ENTRYPOINT ["./bin/vuzoll-ingest-vk-data"]
CMD []
