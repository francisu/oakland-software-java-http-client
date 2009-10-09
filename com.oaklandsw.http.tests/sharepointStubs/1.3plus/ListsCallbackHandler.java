
/**
 * ListsCallbackHandler.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.3  Built on : Aug 10, 2007 (04:45:47 LKT)
 */

    package sharepoint;

    /**
     *  ListsCallbackHandler Callback class, Users can extend this class and implement
     *  their own receiveResult and receiveError methods.
     */
    public abstract class ListsCallbackHandler{



    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public ListsCallbackHandler(Object clientData){
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public ListsCallbackHandler(){
        this.clientData = null;
    }

    /**
     * Get the client data
     */

     public Object getClientData() {
        return clientData;
     }

        
           /**
            * auto generated Axis2 call back method for GetList method
            * override this method for handling normal response from GetList operation
            */
           public void receiveResultGetList(
                    sharepoint.ListsStub.GetListResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from GetList operation
           */
            public void receiveErrorGetList(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for GetListAndView method
            * override this method for handling normal response from GetListAndView operation
            */
           public void receiveResultGetListAndView(
                    sharepoint.ListsStub.GetListAndViewResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from GetListAndView operation
           */
            public void receiveErrorGetListAndView(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for DeleteList method
            * override this method for handling normal response from DeleteList operation
            */
           public void receiveResultDeleteList(
                    sharepoint.ListsStub.DeleteListResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from DeleteList operation
           */
            public void receiveErrorDeleteList(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for AddList method
            * override this method for handling normal response from AddList operation
            */
           public void receiveResultAddList(
                    sharepoint.ListsStub.AddListResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from AddList operation
           */
            public void receiveErrorAddList(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for AddListFromFeature method
            * override this method for handling normal response from AddListFromFeature operation
            */
           public void receiveResultAddListFromFeature(
                    sharepoint.ListsStub.AddListFromFeatureResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from AddListFromFeature operation
           */
            public void receiveErrorAddListFromFeature(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for UpdateList method
            * override this method for handling normal response from UpdateList operation
            */
           public void receiveResultUpdateList(
                    sharepoint.ListsStub.UpdateListResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from UpdateList operation
           */
            public void receiveErrorUpdateList(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for GetListCollection method
            * override this method for handling normal response from GetListCollection operation
            */
           public void receiveResultGetListCollection(
                    sharepoint.ListsStub.GetListCollectionResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from GetListCollection operation
           */
            public void receiveErrorGetListCollection(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for GetListItems method
            * override this method for handling normal response from GetListItems operation
            */
           public void receiveResultGetListItems(
                    sharepoint.ListsStub.GetListItemsResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from GetListItems operation
           */
            public void receiveErrorGetListItems(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for GetListItemChanges method
            * override this method for handling normal response from GetListItemChanges operation
            */
           public void receiveResultGetListItemChanges(
                    sharepoint.ListsStub.GetListItemChangesResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from GetListItemChanges operation
           */
            public void receiveErrorGetListItemChanges(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for GetListItemChangesSinceToken method
            * override this method for handling normal response from GetListItemChangesSinceToken operation
            */
           public void receiveResultGetListItemChangesSinceToken(
                    sharepoint.ListsStub.GetListItemChangesSinceTokenResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from GetListItemChangesSinceToken operation
           */
            public void receiveErrorGetListItemChangesSinceToken(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for UpdateListItems method
            * override this method for handling normal response from UpdateListItems operation
            */
           public void receiveResultUpdateListItems(
                    sharepoint.ListsStub.UpdateListItemsResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from UpdateListItems operation
           */
            public void receiveErrorUpdateListItems(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for AddDiscussionBoardItem method
            * override this method for handling normal response from AddDiscussionBoardItem operation
            */
           public void receiveResultAddDiscussionBoardItem(
                    sharepoint.ListsStub.AddDiscussionBoardItemResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from AddDiscussionBoardItem operation
           */
            public void receiveErrorAddDiscussionBoardItem(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for GetVersionCollection method
            * override this method for handling normal response from GetVersionCollection operation
            */
           public void receiveResultGetVersionCollection(
                    sharepoint.ListsStub.GetVersionCollectionResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from GetVersionCollection operation
           */
            public void receiveErrorGetVersionCollection(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for AddAttachment method
            * override this method for handling normal response from AddAttachment operation
            */
           public void receiveResultAddAttachment(
                    sharepoint.ListsStub.AddAttachmentResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from AddAttachment operation
           */
            public void receiveErrorAddAttachment(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for GetAttachmentCollection method
            * override this method for handling normal response from GetAttachmentCollection operation
            */
           public void receiveResultGetAttachmentCollection(
                    sharepoint.ListsStub.GetAttachmentCollectionResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from GetAttachmentCollection operation
           */
            public void receiveErrorGetAttachmentCollection(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for DeleteAttachment method
            * override this method for handling normal response from DeleteAttachment operation
            */
           public void receiveResultDeleteAttachment(
                    sharepoint.ListsStub.DeleteAttachmentResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from DeleteAttachment operation
           */
            public void receiveErrorDeleteAttachment(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for CheckOutFile method
            * override this method for handling normal response from CheckOutFile operation
            */
           public void receiveResultCheckOutFile(
                    sharepoint.ListsStub.CheckOutFileResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from CheckOutFile operation
           */
            public void receiveErrorCheckOutFile(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for UndoCheckOut method
            * override this method for handling normal response from UndoCheckOut operation
            */
           public void receiveResultUndoCheckOut(
                    sharepoint.ListsStub.UndoCheckOutResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from UndoCheckOut operation
           */
            public void receiveErrorUndoCheckOut(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for CheckInFile method
            * override this method for handling normal response from CheckInFile operation
            */
           public void receiveResultCheckInFile(
                    sharepoint.ListsStub.CheckInFileResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from CheckInFile operation
           */
            public void receiveErrorCheckInFile(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for GetListContentTypes method
            * override this method for handling normal response from GetListContentTypes operation
            */
           public void receiveResultGetListContentTypes(
                    sharepoint.ListsStub.GetListContentTypesResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from GetListContentTypes operation
           */
            public void receiveErrorGetListContentTypes(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for GetListContentType method
            * override this method for handling normal response from GetListContentType operation
            */
           public void receiveResultGetListContentType(
                    sharepoint.ListsStub.GetListContentTypeResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from GetListContentType operation
           */
            public void receiveErrorGetListContentType(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for CreateContentType method
            * override this method for handling normal response from CreateContentType operation
            */
           public void receiveResultCreateContentType(
                    sharepoint.ListsStub.CreateContentTypeResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from CreateContentType operation
           */
            public void receiveErrorCreateContentType(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for UpdateContentType method
            * override this method for handling normal response from UpdateContentType operation
            */
           public void receiveResultUpdateContentType(
                    sharepoint.ListsStub.UpdateContentTypeResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from UpdateContentType operation
           */
            public void receiveErrorUpdateContentType(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for DeleteContentType method
            * override this method for handling normal response from DeleteContentType operation
            */
           public void receiveResultDeleteContentType(
                    sharepoint.ListsStub.DeleteContentTypeResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from DeleteContentType operation
           */
            public void receiveErrorDeleteContentType(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for UpdateContentTypeXmlDocument method
            * override this method for handling normal response from UpdateContentTypeXmlDocument operation
            */
           public void receiveResultUpdateContentTypeXmlDocument(
                    sharepoint.ListsStub.UpdateContentTypeXmlDocumentResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from UpdateContentTypeXmlDocument operation
           */
            public void receiveErrorUpdateContentTypeXmlDocument(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for UpdateContentTypesXmlDocument method
            * override this method for handling normal response from UpdateContentTypesXmlDocument operation
            */
           public void receiveResultUpdateContentTypesXmlDocument(
                    sharepoint.ListsStub.UpdateContentTypesXmlDocumentResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from UpdateContentTypesXmlDocument operation
           */
            public void receiveErrorUpdateContentTypesXmlDocument(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for DeleteContentTypeXmlDocument method
            * override this method for handling normal response from DeleteContentTypeXmlDocument operation
            */
           public void receiveResultDeleteContentTypeXmlDocument(
                    sharepoint.ListsStub.DeleteContentTypeXmlDocumentResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from DeleteContentTypeXmlDocument operation
           */
            public void receiveErrorDeleteContentTypeXmlDocument(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for ApplyContentTypeToList method
            * override this method for handling normal response from ApplyContentTypeToList operation
            */
           public void receiveResultApplyContentTypeToList(
                    sharepoint.ListsStub.ApplyContentTypeToListResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from ApplyContentTypeToList operation
           */
            public void receiveErrorApplyContentTypeToList(java.lang.Exception e) {
            }
                


    }
    