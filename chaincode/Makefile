clean:
	rm -r -f ./auth/build

build-auth:
	cd auth && \
	  docker build -t gradle-build-consent-chaincode-image . && \
	  mkdir -p build && \
	  docker create -ti --name gradle-build-consent-chaincode gradle-build-consent-chaincode-image bash && \
	  docker cp gradle-build-consent-chaincode:/app/build/. build && \
	  docker rm gradle-build-consent-chaincode && \
	  docker rmi gradle-build-consent-chaincode-image
