docker build \
  --build-arg VERSION=6.0.0-PRE_ALPHA-1 \
  --build-arg KTOR_PORT=8080 \
  -t bombies/robertify:6.0.0-PRE_ALPHA-1 .
