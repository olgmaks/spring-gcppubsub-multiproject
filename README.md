# spring-gcppubsub-multiproject

that is how you can connect in scope of one spring app to two GCP projects


steps:

specify projects ids:


edit application.properties:

```
project1.id=<enter first project id here>
project2.id=<enter second project id here>
project1.creds=project1.json
project2.creds=project2.json
````


edit service account credentials in project1.json, project1.json
