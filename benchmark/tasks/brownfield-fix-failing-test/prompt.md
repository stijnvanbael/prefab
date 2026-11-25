This is our web shop (Spring Boot with Prefab). A colleague merged "discontinue a product"
(`POST /products/{id}/discontinuation`, see `ProductDiscontinued` and `StockLevel`) and went on holiday. Since then
the build is red: `StockLevelDiscontinuationIntegrationTest` fails.

Find out why it fails and fix the application. The test describes the intended behaviour: do not change, disable or
delete it. All other tests must keep passing.
