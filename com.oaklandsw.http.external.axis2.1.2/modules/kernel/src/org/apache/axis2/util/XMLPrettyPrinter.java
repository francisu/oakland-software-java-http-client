package org.apache.axis2.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * An XML pretty printer based on jtidy (http://sourceforge.net/projects/jtidy)
 * The Jtidy jar needs to be in classpath for this to work and can be found at
 * http://sourceforge.net/project/showfiles.php?group_id=13153
 */
public class XMLPrettyPrinter {

    private static final Log log = LogFactory.getLog(XMLPrettyPrinter.class);
    private static final String PRETTIFIED_SUFFIX = ".prettyfied";


    /**
     * Pretty prints contents of the java source file.
     *
     * @param file
     */
    public static void prettify(File file) {
        try {
            // Create an instance of the Jalopy bean
            Class clazz = Loader.loadClass("org.w3c.tidy.Tidy");
            Object prettifier = clazz.newInstance();

            //set the input to be xml
            Method setXmlInFlagMethod = clazz.getMethod(
                    "setXmlTags",
                    new Class[]{boolean.class});
            setXmlInFlagMethod.invoke(prettifier,
                                      new Object[]{Boolean.TRUE});

            //set the output to be xml
            Method setXmlOutFlagMethod = clazz.getMethod(
                    "setXmlOut",
                    new Class[]{boolean.class});
            setXmlOutFlagMethod.invoke(prettifier,
                                       new Object[]{Boolean.TRUE});

            //create the input stream
            InputStream input = new FileInputStream(file);

            //create a new file with "prettyfied"  attached
            // to existing file name
            String existingFileName = file.getAbsolutePath();
            String tempFileName = existingFileName + PRETTIFIED_SUFFIX;

            File tempFile = new File(tempFileName);
            FileOutputStream tempFileOutputStream = new FileOutputStream(tempFile);
            //Call the pretty printer
            Method parsr = clazz.getMethod("parse", new Class[]{
                    InputStream.class,
                    OutputStream.class});

            parsr.invoke(prettifier, new Object[]{input,
                    tempFileOutputStream});

            //rename and restore the pretty printed one as the original

            //first close the streams. if not this may cause the
            // files not to be renamed
            input.close();
            tempFileOutputStream.close();
            //delete the original
            file.delete();

            if (!tempFile.renameTo(new File(existingFileName))) {
                throw new Exception("File renaming failed!" + existingFileName);
            }
            log.debug("Pretty printed file : " + file);
        } catch (ClassNotFoundException e) {
            log.debug("Tidy not found - unable to pretty print " + file);
        } catch (Exception e) {
            log.warn("Exception occurred while trying to pretty print file " + file, e);
        } catch (Throwable t) {
            log.debug("Exception occurred while trying to pretty print file " + file, t);
        }

    }


}
