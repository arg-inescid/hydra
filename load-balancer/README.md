# Quick Start

```nginx
upstream backends {
    zone zone_for_backends 1m;
    server 127.0.0.1:6001;
    server 127.0.0.1:6002;
    server 127.0.0.1:6003;
}

server {
    listen 6000;

    location /dynamic {
		allow 127.0.0.1;
	    deny all;
        dynamic_upstream;
    }

    location / {
	    proxy_pass http://backends;
    }
}
```

# Load balancer APIs

You can operate upstreams dynamically with HTTP APIs.

## list

```bash
$ curl "http://127.0.0.1:6000/dynamic?upstream=zone_for_backends"
server 127.0.0.1:6001;
server 127.0.0.1:6002;
server 127.0.0.1:6003;
$
```

## verbose

```bash
$ curl "http://127.0.0.1:6000/dynamic?upstream=zone_for_backends&verbose="
server 127.0.0.1:6001 weight=1 max_fails=1 fail_timeout=10;
server 127.0.0.1:6002 weight=1 max_fails=1 fail_timeout=10;
server 127.0.0.1:6003 weight=1 max_fails=1 fail_timeout=10;
$
```

## update_parameters

```bash
$ curl "http://127.0.0.1:6000/dynamic?upstream=zone_for_backends&server=127.0.0.1:6003&weight=10&max_fails=5&fail_timeout=5"
server 127.0.0.1:6001 weight=1 max_fails=1 fail_timeout=10;
server 127.0.0.1:6002 weight=1 max_fails=1 fail_timeout=10;
server 127.0.0.1:6003 weight=10 max_fails=5 fail_timeout=5;
$
```

The supported parameters are blow.

 * weight
 * max_fails
 * fail_timeout

## down

```bash
$ curl "http://127.0.0.1:6000/dynamic?upstream=zone_for_backends&server=127.0.0.1:6003&down="
server 127.0.0.1:6001 weight=1 max_fails=1 fail_timeout=10;
server 127.0.0.1:6002 weight=1 max_fails=1 fail_timeout=10;
server 127.0.0.1:6003 weight=1 max_fails=1 fail_timeout=10 down;
$
```

## up

```bash
$ curl "http://127.0.0.1:6000/dynamic?upstream=zone_for_backends&server=127.0.0.1:6003&up="
server 127.0.0.1:6001 weight=1 max_fails=1 fail_timeout=10;
server 127.0.0.1:6002 weight=1 max_fails=1 fail_timeout=10;
server 127.0.0.1:6003 weight=1 max_fails=1 fail_timeout=10;
$
```

## add

```bash
$ curl "http://127.0.0.1:6000/dynamic?upstream=zone_for_backends&add=&server=127.0.0.1:6004"
server 127.0.0.1:6001;
server 127.0.0.1:6002;
server 127.0.0.1:6003;
server 127.0.0.1:6004;
$
```

## remove

```bash
$ curl "http://127.0.0.1:6000/dynamic?upstream=zone_for_backends&remove=&server=127.0.0.1:6003"
server 127.0.0.1:6001;
server 127.0.0.1:6002;
server 127.0.0.1:6004;
$
```
