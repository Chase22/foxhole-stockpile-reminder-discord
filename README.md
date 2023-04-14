## Properties
Can either be supplies via environment variable or via the `src/main/resources/koin.properties` file

| name            | type   | description                                    |
|-----------------|--------|------------------------------------------------|
| fostorChannelId | Long   | The id of the channel fostor should operate in |
| kortToken       | String | The discord api token                          |

## Docker properties
In order to properly build and push docker containers you need to create `local.properties` file
```properties
repository=my-repository.org
```