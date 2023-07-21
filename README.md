# Plugin for Conversations in OpenSearch
This repo is a WIP plugin for handling conversations in OpenSearch ([Per this RFC](https://github.com/opensearch-project/ml-commons/issues/1150)).
It consists of three components: 
 - A notion of conversational memory, storage, and a CRUD API. _(This is all that's in here today)_
 - A series of search processors and pipelines that calls externally hosted LLMs for inference. _(This might go somewhere else)_
 - A simple API that wraps those components for developers who don't want to learn the ins and outs of the most modern version of OpenSearch. _(This might also go somewhere else)_

## Progress so far
Currently, we've mostly done the CRUD API for conversational memory - it's in the [conversational-memory](https://github.com/aryn-ai/conversational-opensearch/tree/conversational-memory) branch.
The search pipeline is still a work in progress, and the wrapper API is nonexistant. These parts might not go in this repo.

---
---
This repo was create using the OpenSearch plugin template ([found here](https://github.com/opensearch-project/opensearch-plugin-template-java)).
The template came with:
 - Some integration tests
 - Some boilerplate plugin and unittest code
 - `build.gradle`
 - Notice and License files (Apache 2.0)

## Naming
This plugin is named `conversational`, because it's a conversational plugin but "plugin" shouldn't be part of a plugin's name. 
Likewise "opensearch" shouldn't be part of a plugin's name, but it's in the title of this repo because this is an OpenSearch project.
Accordingly, all code lives somewhere in the `org.opensearch.conversational` package.

## Tests
The `conversational-memory` branch has, along with the CRUD API, numerous tests.
I'm pretty sure they're not completely comprehensive, but they're pretty good.
Run them with 

```
./gradlew check
```

The current implementation against OpenSearch 2.8.0 - there shouldn't be too much incompatibility between versions given what currently exists, but when pipelines come along that assertion will break.



## License
This code is licensed under the Apache 2.0 License. See [LICENSE.txt](LICENSE.txt).

## Copyright
Copyright Aryn, Inc 2023. See [NOTICE](NOTICE.txt) for details.
