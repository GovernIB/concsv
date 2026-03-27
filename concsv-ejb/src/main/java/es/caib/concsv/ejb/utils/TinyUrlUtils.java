package es.caib.concsv.ejb.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import es.caib.comanda.model.v1.salut.IntegracioApp;
import es.caib.concsv.ejb.helpers.IntegracionsHelper;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;

public class TinyUrlUtils {
    public static String doTinyUrl(String url, IntegracionsHelper integracionsHelper) {
		long t0 = System.currentTimeMillis();
    	Logger log = Logger.getLogger(TinyUrlUtils.class);
    	
    	String input = "{url : \""+ url +"\"}";
        String tinyRestURL = ConfigProvider.getConfig().getValue("es.caib.concsv.tiny.rest.url", String.class);
        String username = ConfigProvider.getConfig().getValue("es.caib.concsv.tiny.username", String.class);
        String password = ConfigProvider.getConfig().getValue("es.caib.concsv.tiny.password", String.class);
        String authString = username + ":" + password;
        //String authStringEnc = new BASE64Encoder().encode(authString.getBytes());
        String authStringEnc = "";
		try {
			authStringEnc = new String (Base64.encodeBase64(authString.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e1) {
			log.error(e1);
		}
        Client client = Client.create();
        WebResource webResource = client.resource(tinyRestURL+"/encode");
        ClientResponse response = webResource
                .accept("application/json")
                .header("Authorization", "Basic " + authStringEnc)
                .type(MediaType.TEXT_PLAIN)
                .post(ClientResponse.class, input);
        String output = response.getEntity(String.class);
        if (response.getStatus() == 200 && !output.contains("error")) {
            try {
            	ObjectMapper mapper = new ObjectMapper(); 
            	JsonNode jsonNode = mapper.readTree(output);
                String alias = jsonNode.get("alias").asText();
                String baseURL = jsonNode.get("baseURL").asText();
				integracionsHelper.addSuccessOperation(IntegracioApp.TIN, System.currentTimeMillis() - t0);
                return baseURL+"/"+alias;
            } catch (Exception e) {
                //doTiny = false;
				integracionsHelper.addErrorOperation(IntegracioApp.TIN);
            	log.error("TinyCAIB failed parsing JSON Response : Error message : "+e.getMessage());
            }
        } else {
			integracionsHelper.addErrorOperation(IntegracioApp.TIN);
            //doTiny = false;
            if (output.contains("error")) {
                log.error("TinyCAIB failed parsing JSON Response : "+output + " url:" + url);
            } else {
                log.error("TinyCAIB failed HTTP error code : "+ response.getStatus() + " url:" + url);
            }
        }
        return url;
    }
}
