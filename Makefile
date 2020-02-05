#
# Makefile to manage this project's development environment.
#

TAG=luontola/retrolambda-dev
NAME=wrapping-retrolambda-shell
DATE=$(shell date +"%Y-%m-%d")

# Directory that this Makefile is in
mkfile_path := $(abspath $(lastword $(MAKEFILE_LIST)))
current_path := $(dir $(mkfile_path))

# Builds the development docker file
docker-build:
	rocker build --var Tag=$(TAG) --var Date=$(DATE) --file dev/Rockerfile

# Clean this docker image
docker-clean:
	-docker rmi $(TAG)

# Start a development shell
shell:
	mkdir -p ~/.m2
	docker run --rm \
		--name=$(NAME) \
		-P=true \
		-v ~/.m2/repository:/root/.m2/repository \
		-v ~/.m2/settings.xml:/root/.m2/settings.xml \
		-v $(current_path):/project \
		-it $(TAG) /bin/bash

# Attach a new terminal to the already running shell
shell-attach:
	docker exec -it -u=$(USER) $(NAME) /bin/bash
