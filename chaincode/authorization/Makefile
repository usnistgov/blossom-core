build-auth:
	rm -r -f ./build && \
	rm -r -f ./libs && \
	docker build -t gradle-build-authorization-chaincode-image . && \
	mkdir -p ./build && \
	mkdir -p ./libs && \
	docker create -ti --name gradle-build-authorization-chaincode gradle-build-authorization-chaincode-image bash && \
	docker cp gradle-build-authorization-chaincode:/authorization/build/. ./build && \
	docker cp gradle-build-authorization-chaincode:/authorization/libs/. ./libs && \
	docker rm gradle-build-authorization-chaincode && \
	docker rmi gradle-build-authorization-chaincode-image
