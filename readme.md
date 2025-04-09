# URL Shortener

An easy to use url shortener with a http "api" written in java, running inside a container.

## Used libraries

- RocksDB key-value store (https://github.com/facebook/rocksdb/)
- Undertow http server (https://github.com/undertow-io/undertow)

## Build Docker image

```shell
docker build -t urlshortener .
```

## Run Docker container

```shell
docker run -it --rm -p 8080:80 -e BASEURL=http://localhost:8080/ urlshortener
```

## Config

Configuration is done via environment variables.

There are two config options.

- `BASEURL` the base url of the hosted url shortening service. If `BASEURL=https://shorturl.example.com/`
then the server will return `https://shorturl.example.com/RANDOM_ID_HERE`. If not set, the server will
only return the random id of the shortened url.

- `DOMAINS` the accepted domains by the server. Separate multiple domains with a comma. If the domain starts
with a dot then subdomains are also accepted. If not set, the server will accept all domains.

Example:
```
DOMAINS=.wikipedia.org,google.com,www.google.com
```
Will accept `wikipedia.org` and any subdomains (e.g. `en.wikipedia.org`). `google.com` and `www.google.com`
but NOT `about.google.com` or `example.com`.

## Data directories/volumes

- `/db` contains the RocksDB database
- `/db-log` contains the logs from RocksDB

## Complete example

```shell
docker run -d --name urlshortener -p 8080:80 -v urlshortener-db:/db -v urlshortener-db-log:/db-log -e DOMAINS=.wikipedia.org,google.com,www.google.com -e BASEURL=http://localhost:8080/ urlshortener
```

## API

- `/` the http server serves a simple form for shortening urls
- `/ping` a debug/health check returning `pong` and the current time
- `/short?url=LONG_URL_HERE` creates a short url for the given url (a-z,A-Z)
- `/short?caseInsensitive&url=LONG_URL_HERE` creates a case-insensitive short url for the given url (a-z, but may also be converted to upper case)
- `/URL_ID_HERE` resolves the short url to the actual url (303 redirect)
