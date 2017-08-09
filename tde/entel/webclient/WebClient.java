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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Created by Rene on 7/24/2017.
 */
public class WebClient {

    protected String soapUIPath = "";
    protected String operationName = "";
    protected String endpointURI = "";
    protected String requestPayload = "";
    protected String connectionURI = "";
    protected String user = "";
    protected String pass = "";

    public WebClient() {

    }

    public String readFile(String path, Charset encoding) throws IOException {
        //String content = readFile("test.txt", StandardCharsets.UTF_8);
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public boolean sendRequest() {
        String result = "";
        String projectFile = soapUIPath;
        WsdlProject project = null;
        Date actualDate = new Date();
        SimpleDateFormat sdfForFile = new SimpleDateFormat("yyyy/MM/dd:HH:mm:ss");
        long startTime = 0;
        long stopTime = 0;
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
            startTime = System.currentTimeMillis();
            WsdlSubmitContext wsdlSubmitContext = new WsdlSubmitContext(req);
            WsdlSubmit<?> submit = (WsdlSubmit<?>) req.submit(wsdlSubmitContext, false);
            Response response = submit.getResponse();
            stopTime = System.currentTimeMillis();
            result = response.getContentAsString();
            if (result.contains("ERROR") && result.contains("FRSP")) {

            } else {
                result = " OK";
            }

            System.out.println("The result =" + result);

            long time = stopTime - startTime;
            System.out.println("The time = "+ time);
            dbTrace(projectFile, operationName, time, result, sdfForFile.format(actualDate));
        } catch (Exception e) {
            stopTime = System.currentTimeMillis();
            long time = stopTime - startTime;
            System.out.println(time);
            String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
            dbTrace(projectFile, operationName, time, fullStackTrace, sdfForFile.format(actualDate));
            e.printStackTrace();
        }
        return result.equals("OK");
    }

    public void dbTrace(String serviceName, String operationName, long time, String response, String date) {

        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection(connectionURI, user, pass);
            String query =
                "Insert into ESB_MONITORING (SERVICE_NAME, OPERATION_NAME, TIME, RESPONSE, REQ_DATE) values (?,?,?,?,?)";
            pstmt = connection.prepareStatement(query); // create a statement
            pstmt.setString(1, serviceName);
            pstmt.setString(2, operationName);
            pstmt.setLong(3, time);
            pstmt.setString(4, response);
            pstmt.setString(5, date);
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
        try {
            Properties prop = new Properties();
            String propFileName = "client.properties";
            propFileName = "C:\\Users\\proyecto\\Documents\\Work\\WebClient\\conf\\client.properties";
            InputStream inputStream = new FileInputStream(propFileName);
            if (inputStream != null) {
                prop.load(inputStream);
                client.soapUIPath = prop.getProperty("soapUIPath");
                client.endpointURI = prop.getProperty("endpointURI");
                client.requestPayload = prop.getProperty("requestPayload");
                client.connectionURI = prop.getProperty("connectionURI");
                client.user = prop.getProperty("user");
                client.pass = prop.getProperty("pass");
                client.operationName = prop.getProperty("operationName");
            }
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
        }
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-p")) {
                    client.soapUIPath = args[i + 1];
                }
                if (args[i].equals("-e")) {
                    client.endpointURI = args[i + 1];
                }
                if (args[i].equals("-o")) {
                    client.operationName = args[i + 1];
                }
                if (args[i].equals("-r")) {
                    client.requestPayload = args[i + 1];
                }
            }
        }
        if (client.sendRequest())
            System.exit(0);
        else
            System.exit(1);
    }
}
