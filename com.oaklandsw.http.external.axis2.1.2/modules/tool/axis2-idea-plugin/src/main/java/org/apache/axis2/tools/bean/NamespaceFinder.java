package org.apache.axis2.tools.bean;


public class NamespaceFinder {
    private static String NS_PREFIX = "http://";
    private static String SCHEMA_NS_SUFFIX = "/xsd";
    private static String SCHEMA_NS_DEFAULT_PREFIX = "xsd";
    private static String NS_DEFAULT_PREFIX = "ns";


    public static String getTargetNamespaceFromClass(String fullyQualifiedClassName){
        //tokenize the className
        String[] classNameParts = fullyQualifiedClassName.split("\\.");
        //add the strings in reverse order to make the namespace
        String nsUri = "";
        for(int i=classNameParts.length-1;i>=0;i--){
            nsUri = nsUri + classNameParts[i] + (i==0?"":".");
        }

        return NS_PREFIX + nsUri;

    }

    public static String getSchemaTargetNamespaceFromClass(String fullyQualifiedClassName){
        return getTargetNamespaceFromClass(fullyQualifiedClassName) +SCHEMA_NS_SUFFIX;
    }

    public static String getDefaultSchemaNamespacePrefix(){
        return SCHEMA_NS_DEFAULT_PREFIX;
    }

    public static String getDefaultNamespacePrefix(){
        return NS_DEFAULT_PREFIX;
    }

    public static String getServiceNameText(String fullyQualifiedClassName){
        //tokenize the className
        String[] classNameParts = fullyQualifiedClassName.split("\\.");
        return classNameParts[classNameParts.length-1];
    }
}
