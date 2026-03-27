package test.caib.concsv.front;

public class Test {
	
    /*public static void main(String[] args) {
    	
    	
        String dstPath = "c:/rest_file_"+System.currentTimeMillis()+".pdf";
        String oldHash =  "14691707027028622093360140201307996";
        String metadata1 =  "Metadata uno";
        String metadata2 =  "Metadata dos";
        String watermark =  "Marca de agua";

        try {
            ClientConfig clientConfig = new ClientConfig();
            clientConfig.register(JacksonFeature.class);
            Client client = ClientBuilder.newBuilder().withConfig(clientConfig).build();

            WebTarget webTarget = client.target("http://epreinf23.caib.es:8080/concsv/rest/printable/"+oldHash);
            webTarget = webTarget.queryParam("metadata1",  metadata1);
            webTarget = webTarget.queryParam("metadata2",  metadata2);
            webTarget = webTarget.queryParam("watermark",  watermark);

            ClientResponse response = webTarget.request(MediaType.APPLICATION_JSON).accept("application/octect-stream").get(ClientResponse.class);

            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "+response.getStatus());
            }
            byte[] output = response.readEntity(byte[].class);
            FileUtils.writeByteArrayToFile(new File(dstPath), output);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }*/
}
