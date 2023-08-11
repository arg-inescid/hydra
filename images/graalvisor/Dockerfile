FROM debian
ADD disk/ /
RUN apt update

# Because of jvips
RUN apt -y install libgomp1 libexpat-dev liblcms2-dev libwebpmux3 libwebpdemux2 libwebp-dev libimagequant-dev libexif-dev libglibd-2.0-dev
# Because of python classify (pytorch)
RUN apt -y install libopenmpi-dev
ENTRYPOINT /polyglot-proxy
