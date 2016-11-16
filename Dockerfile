FROM python:2-onbuild

RUN mkdir /data
VOLUME /data

CMD [ "python", "ingest-vk-data.py" ]
