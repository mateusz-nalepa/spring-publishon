1. Run `MockServerApp`
2. Run `TestApp`
3. Execute one of the curl commands and look for the `TestApp` logs

- Publish response on another Thread
```shell
curl localhost:8080/data-with-publishon
```

- Publish response on http thread
```shell
curl localhost:8080/data-without-publishon
```
