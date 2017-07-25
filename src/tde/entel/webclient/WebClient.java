package tde.entel.webclient;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.StandaloneSoapUICore;
import com.eviware.soapui.impl.wsdl.*;
import com.eviware.soapui.model.iface.Response;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Created by Rene on 7/24/2017.
 */
public class WebClient {

    private String soapUIPath = "";
    private String operationName = "";
    private String endpointURI = "";
    private String requestPayload = "";
    private String connectionURI = "";
    private String user = "";
    private String pass = "";

    public WebClient() {
        try {
            Properties prop = new Properties();
            String propFileName = "client.properties";
            propFileName = "D:\\Work\\WebClient\\conf\\client.properties";
            InputStream inputStream = new FileInputStream(propFileName);
            if (inputStream != null) {
                prop.load(inputStream);
                soapUIPath = prop.getProperty("soapUIPath");
                endpointURI = prop.getProperty("endpointURI");
                requestPayload = prop.getProperty("requestPayload");
                connectionURI = prop.getProperty("connectionURI");
                user = prop.getProperty("user");
                pass = prop.getProperty("pass");
                operationName = prop.getProperty("operationName");
            }
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
        }
    }

    public String readFile(String path, Charset encoding) throws IOException {
        //String content = readFile("test.txt", StandardCharsets.UTF_8);
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public void sendRequest() {
        String projectFile = soapUIPath;
        WsdlProject project = null;
        try {
            project = new WsdlProject(projectFile);

            WsdlInterface wsdl = (WsdlInterface) project.getInterfaceAt(0);
            String soapVersion = wsdl.getSoapVersion().toString();
            WsdlOperation op = (WsdlOperation) wsdl.getOperationByName(operationName);
            //WsdlRequest req = op.getRequestByName("Req_" + soapVersion + "_" + "login");
            WsdlRequest req = op.addNewRequest("Request");
            //req.setRequestContent(op.createRequest(true));
            String content = readFile(requestPayload, StandardCharsets.UTF_8);
            req.setRequestContent(content);
            req.setEndpoint(endpointURI);
            WsdlSubmitContext wsdlSubmitContext = new WsdlSubmitContext(req);
            WsdlSubmit<?> submit = (WsdlSubmit<?>) req.submit(wsdlSubmitContext, false);
            Response response = submit.getResponse();
            String result = response.getContentAsString();

            System.out.println("The result =" + result);
            Date actualDate = new Date();
            SimpleDateFormat sdfForFile = new SimpleDateFormat("yyyyMMddHHmmss");
            dbTrace(result, sdfForFile.format(actualDate));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dbTrace(String value1, String value2) {

        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = DriverManager.getConnection(connectionURI, user, pass);
            String query = "Insert into someTable (ID, NAME) values (?,?)";
            pstmt = connection.prepareStatement(query); // create a statement
            pstmt.setString(1, value1);
            pstmt.setString(2, value2);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void runSoap() throws Exception {

        String projectFile = "C:/Test/TestProjectA-soapui-project.xml";
        SoapUI.setSoapUICore(new StandaloneSoapUICore(true));
        WsdlProject project = new WsdlProject(projectFile);
        int c = project.getInterfaceCount();

        for (int i = 0; i < c; i++) {
            WsdlInterface wsdl = (WsdlInterface) project.getInterfaceAt(i);
            String soapVersion = wsdl.getSoapVersion().toString();

            System.out.println("The SOAP version =" + soapVersion);
            System.out.println("The binding name = " + wsdl.getBindingName());

            int opc = wsdl.getOperationCount();
            System.out.println("Operation count =" + opc);

            String result = "";

            for (int j = 0; j < opc; j++) {
                WsdlOperation op = wsdl.getOperationAt(j);
                String opName = op.getName();

                System.out.println("OPERATION:" + opName);

                WsdlRequest req = op.getRequestByName("Req_" + soapVersion + "_" + opName);

                req.setEndpoint("<my_WSDL_ENDPOINT>");

                WsdlSubmitContext wsdlSubmitContext = new WsdlSubmitContext(req);
                WsdlSubmit<?> submit = (WsdlSubmit<?>) req.submit(wsdlSubmitContext, false);
                Response response = submit.getResponse();
                result = response.getContentAsString();

                System.out.println("The result =" + result);

            }

        }
    }

    public static void main(String[] args) {
        WebClient client = new WebClient();
        client.sendRequest();
    }

}
