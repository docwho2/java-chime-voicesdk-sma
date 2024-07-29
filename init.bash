#!/bin/bash

# Run install at parent POM level so it can build Libraries and install them
mvn install -DskipTests
