clean-auth-cc:
	rm -r -f ./authorization/build

build-auth-cc:
	  docker build -t gradle-build-authorization-chaincode-image --build-arg CC_NAME=authorization . && \
	  mkdir -p authorization/build && \
	  docker create -ti --name gradle-build-authorization-chaincode gradle-build-authorization-chaincode-image bash && \
	  docker cp gradle-build-authorization-chaincode:/authorization/build/. authorization/build && \
	  docker rm gradle-build-authorization-chaincode && \
	  docker rmi gradle-build-authorization-chaincode-image
