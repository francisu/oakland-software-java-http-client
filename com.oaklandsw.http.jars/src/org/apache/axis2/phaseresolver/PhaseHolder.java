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


package org.apache.axis2.phaseresolver;

import org.apache.axis2.deployment.DeploymentErrorMsgs;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.i18n.Messages;

import java.util.ArrayList;

/**
 * This class hold all the phases found in the services.xml and server.xml
 */
public class PhaseHolder {
    private ArrayList phaseList;

    public PhaseHolder() {
    }

    public PhaseHolder(ArrayList phases) {
        this.phaseList = phases;
    }

    /**
     * If the phase name is equal to "*" that implies , the handler should be
     * added to each and every phase in the system for a given flow  , and at that
     * point if the phase rule contains any before or aftere then they will be
     * ignored. Phase first and phase last are supported , but make sure you dont
     * break any of the phase rules.
     * <p/>
     * If the phase name is not above then the hadler will be added to the phase
     * specified by  the phase rule , and no rules will be ignored.
     *
     * @param handlerDesc
     * @throws PhaseException
     */
    public void addHandler(HandlerDescription handlerDesc) throws PhaseException {
        String phaseName = handlerDesc.getRules().getPhaseName();
        if (Phase.ALL_PHASES.equals(phaseName)) {
            handlerDesc.getRules().setBefore("");
            handlerDesc.getRules().setAfter("");
            for (int i = 0; i < phaseList.size(); i++) {
                Phase phase = (Phase) phaseList.get(i);
                phase.addHandler(handlerDesc);
            }
        } else {
            if (isPhaseExist(phaseName)) {
                getPhase(phaseName).addHandler(handlerDesc);
            } else {
                throw new PhaseException(Messages.getMessage(DeploymentErrorMsgs.INVALID_PHASE,
                                                             phaseName, handlerDesc.getName()));
            }
        }
    }

    /**
     * this method is used to get the actual phase object given in the phase array list
     *
     * @param phaseName
     */
    private Phase getPhase(String phaseName) {
        for (int i = 0; i < phaseList.size(); i++) {
            Phase phase = (Phase) phaseList.get(i);

            if (phase.getPhaseName().equals(phaseName)) {
                return phase;
            }
        }

        return null;
    }

    /**
     * Method isPhaseExist
     *
     * @param phaseName
     */
    private boolean isPhaseExist(String phaseName) {
        for (int i = 0; i < phaseList.size(); i++) {
            Phase phase = (Phase) phaseList.get(i);

            if (phase.getPhaseName().equals(phaseName)) {
                return true;
            }
        }

        return false;
    }
}
