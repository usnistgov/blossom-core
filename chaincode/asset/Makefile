build-asset:
	rm -r -f ./build && \
	rm -r -f ./libs && \
	docker build -t gradle-build-asset-chaincode-image . && \
	mkdir -p ./build && \
	mkdir -p ./libs && \
	docker create -ti --name gradle-build-asset-chaincode gradle-build-asset-chaincode-image bash && \
	docker cp gradle-build-asset-chaincode:/asset/build/. ./build && \
	docker cp gradle-build-asset-chaincode:/asset/libs/. ./libs && \
	docker rm gradle-build-asset-chaincode && \
	docker rmi gradle-build-asset-chaincode-image
