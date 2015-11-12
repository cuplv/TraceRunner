package edu.colorado.plv;

import com.sun.xml.internal.bind.api.Bridge;
import com.sun.xml.internal.ws.api.model.CheckedException;
import com.sun.xml.internal.ws.api.model.ExceptionType;
import com.sun.xml.internal.ws.api.model.JavaMethod;
import com.sun.xml.internal.ws.api.model.SEIModel;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by s on 11/10/15.
 * Class to take a stream of events and split them up based on involved objects.
 */
public class DataProjection {
    private Map<MObject, List<CallbackOuterClass.EventInCallback>> objects = new HashMap<>();
    /**
     * object with just type and id counting for hashcode and eq
     */

    public static class MObject{
        private long id;
        /**
         * empty string if other type, check PValue
         */
        private String type;
        private CallbackOuterClass.PValue pValue;
        private static MObject fromPValue(CallbackOuterClass.PValue pvalue){
            if(pvalue.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.POBJCTREFERENC)){
                return new MObject(pvalue);
            }else{
                return null;
            }
        }
        private MObject(CallbackOuterClass.PValue pValue){
            this.pValue = pValue;
            CallbackOuterClass.PValue.ValueTypeCase vtc = pValue.getValueTypeCase();
            if(vtc == CallbackOuterClass.PValue.ValueTypeCase.POBJCTREFERENC){
                CallbackOuterClass.PObjectReference pObjectReference = pValue.getPObjctReferenc();
                this.type = pObjectReference.getType();
                this.id = pObjectReference.getId();
            }else {
                throw new IllegalArgumentException("MObject only for object values");
            }
        }
        @Override
        public int hashCode(){
            return (new Long(id%Integer.MAX_VALUE)).intValue();
        }
        @Override
        public boolean equals(Object object){
            if(object instanceof MObject){
                MObject mObject = (MObject) object;
                return this.id == mObject.getId() &&
                        this.pValue.getValueTypeCase().equals(mObject.getpValue().getValueTypeCase()) &&
                        this.type.equals(mObject.getType());
            }else{
                return false;
            }
        }


        public long getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public CallbackOuterClass.PValue getpValue() {
            return pValue;
        }
    }

    public static DataProjection fromFileInputStream(FileInputStream fileInputStream) throws IOException {
        List<CallbackOuterClass.EventInCallback> events = TraceUtilities.deserialize(fileInputStream);
        return fromEventList(events);
    }
    public static DataProjection fromEventList(List<CallbackOuterClass.EventInCallback> eventsInCallback){
        DataProjection dataProjection = new DataProjection();

        //Get all involved objects
        for(CallbackOuterClass.EventInCallback e : eventsInCallback){
            if(!isMileMarker(e)) {
                for (MObject mObject : allObjectsInEvent(e)) {
                    if(mObject != null) {
                        dataProjection.objects.put(mObject, new ArrayList<CallbackOuterClass.EventInCallback>());
                    }
                }
            }
        }

        for(CallbackOuterClass.EventInCallback e : eventsInCallback){
            //TODO:check if milemarker event (Events that stay regardless of involved objects)
            if(isMileMarker(e)){
                for(List<CallbackOuterClass.EventInCallback> l : dataProjection.objects.values()){
                    l.add(e);
                }
            }
            //TODO:add event to each MObjects in map
            if(!isMileMarker(e)) {
                Set<MObject> mObjects = allObjectsInEvent(e);
                for (MObject mObject : mObjects) {
                    if(e == null){
                        throw new IllegalStateException("object is null");
                    }

                    List<CallbackOuterClass.EventInCallback> event = dataProjection.objects.get(mObject);
                    if(event == null){
                        throw new IllegalStateException("event list is null");
                    }
                    event.add(e);
                }
            }
        }
        return dataProjection;
    }
    public static boolean isMileMarker(CallbackOuterClass.EventInCallback e){
        if(e.getEventTypeCase().equals(CallbackOuterClass.EventInCallback.EventTypeCase.CALLBACK)) {
            return true;
        }
        if(e.getEventTypeCase().equals(CallbackOuterClass.EventInCallback.EventTypeCase.EXCEPTIONEVENT)) {
            return true;
        }
        if(e.getEventTypeCase().compareTo(CallbackOuterClass.EventInCallback.EventTypeCase.LOGE) == 0){
            return true;
        }
        return false;
    }
    public static Set<MObject> allObjectsInEvent(CallbackOuterClass.EventInCallback eventInCallback){
        if(eventInCallback.getEventTypeCase().equals(CallbackOuterClass.EventInCallback.EventTypeCase.CALLBACK)) {
            CallbackOuterClass.Callback callback = eventInCallback.getCallback();
            throw new UnsupportedOperationException();
        }else if(eventInCallback.getEventTypeCase()
                .equals(CallbackOuterClass.EventInCallback.EventTypeCase.METHODEVENT)){
            CallbackOuterClass.MethodEvent methodEvent = eventInCallback.getMethodEvent();

            Set<MObject> mObjects = new HashSet<>();
            List<CallbackOuterClass.PValue> params = methodEvent.getParametersList();
            for( CallbackOuterClass.PValue pValue :params){
                MObject mObject = MObject.fromPValue(pValue);
                if(mObject != null) {
                    mObjects.add(mObject);
                }
            }


            MObject calle = MObject.fromPValue(methodEvent.getCalle());
            if(calle != null) {
                mObjects.add(calle);
            }


            return mObjects;
        }else if(eventInCallback.getEventTypeCase()
                .equals(CallbackOuterClass.EventInCallback.EventTypeCase.EXCEPTIONEVENT)){
            throw new UnsupportedOperationException();
            //return new HashSet<>();
        }

        throw new UnsupportedOperationException();

    }
    public Set<CallbackOuterClass.PValue> getInvolvedObjects(){
        Set<CallbackOuterClass.PValue> involvedObjects = new HashSet<>();
        for(MObject m :objects.keySet()){
            if(m != null) {
                involvedObjects.add(m.getpValue());
            }else{
                involvedObjects.add(null);
            }
        }
        return involvedObjects;
    }
}
