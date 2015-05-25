/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.jaxb;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple cache of JAXB {@link Marshaller} instances.
 *
 * @author Eric Trautman
 */
public class MarshallerCache {

    private Map<Class, Marshaller> classToMarshallerMap;

    public MarshallerCache() {
        this.classToMarshallerMap = new HashMap<Class, Marshaller>();
    }

    public void marshal(Object object,
                        OutputStream outputStream) throws JAXBException {
        Class clazz = object.getClass();
        Marshaller marshaller = classToMarshallerMap.get(clazz);
        if (marshaller == null) {
            marshaller = createAndAddMarshallerToMap(clazz);
        }
        marshaller.marshal(object, outputStream);
    }

    /**
     * Creates and adds a marshaller instance for the specified class
     * to the cached map of instances.
     *
     * @param  clazz  class of objects to be handled by marshaller.
     *
     * @return created marshaller instance.
     *
     * @throws javax.xml.bind.JAXBException
     *   if a marshaller cannot be created.
     */
    private Marshaller createAndAddMarshallerToMap(Class clazz)
            throws JAXBException {
        Marshaller marshaller;
        JAXBContext context = JAXBContext.newInstance(clazz);
        marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT,
                               Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                               Boolean.TRUE);
        classToMarshallerMap.put(clazz, marshaller);
        return marshaller;
    }

}