# Trashcan

Temporary file storage, but you can choose when to expire (delete) like your trashcan!

Q: This application is just a simple file upload right?  
A: Yes, but uploaded file will be encrypted on server

Q: Why encrypt a file at server side  
A: To prevent unwelcome guest who can access your server to see original file content
(it's not that secure just a basic encryption using AES but keygen is very weak)

## Before start

This project using `Spring Boot Webflux` + `Kotlin` + `Postgres` most blocking code are wrapped in coroutines!   
current implementation is very simple to use postgres, in future it will have much more complex feature!

## Quick start

You need to install this

+ Docker
+ Docker Compose

```shell
docker compose up -d
# spring dependency is very big it will need sometime to build
# default port is 8080
# you can visit http://localhost:8080 to use it!
```

## Version

+ V0 : Simple version without any authentication (but can filter local request)
+ V1 : (coming soon)
    + Authentication
        + List uploaded file
        + Remove or edit when to expire
    + Custom password protected

## Note:

+ Quota limit have concurrency issue if request single file from multiple client at single time
  it may increase quota count (unpredictable)