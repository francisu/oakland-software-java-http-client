<%@ WebService Language="C#" Class="Example1" %>
 
using System.Web.Services;
 
[WebService(Namespace="urn:Example1")]
public class Example1 {
 
    [ WebMethod ]
    public string sayHello(string name) {
        return "Hello " + name;
    }
 
}
