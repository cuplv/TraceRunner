package edu.colorado.plv;


import org.json.simple.JSONObject;

import java.io.*;
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
    private final String appPackageGlob;

    public DataProjection(String appPackageGlob) {
        this.appPackageGlob = appPackageGlob;
    }

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

    public static DataProjection fromFileInputStream(FileInputStream fileInputStream,
                                                     String appPackageGlob) throws IOException {
        List<CallbackOuterClass.EventInCallback> events = TraceUtilities.deserialize(fileInputStream);
        return fromEventList(events, appPackageGlob);
    }
    public static DataProjection fromEventList(List<CallbackOuterClass.EventInCallback> eventsInCallback,
                                               String appPackageGlob){
        DataProjection dataProjection = new DataProjection(appPackageGlob);

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
        dataProjection.generateNestedTraces();
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
    public List<CallbackOuterClass.EventInCallback> filterExceptionsFromEvents(
            List<CallbackOuterClass.EventInCallback> events){
        boolean inRelevantCall = false;
        List<CallbackOuterClass.EventInCallback> out = new ArrayList<>();
        for(CallbackOuterClass.EventInCallback evt : events){
            if(evt.getEventTypeCase().equals(CallbackOuterClass.EventInCallback.EventTypeCase.METHODEVENT)){
                if(evt.getMethodEvent().getEventType().equals(CallbackOuterClass.EventType.METHODENTRY)){
                    inRelevantCall = true;
                }
                if(evt.getMethodEvent().getEventType().equals(CallbackOuterClass.EventType.METHODEXIT)){
                    inRelevantCall = false;
                }
            }
            if(evt.getEventTypeCase().equals(CallbackOuterClass.EventInCallback.EventTypeCase.EXCEPTIONEVENT)){
                if(inRelevantCall){
                    out.add(evt);
                }
            }else if(evt.getEventTypeCase().equals(CallbackOuterClass.EventInCallback.EventTypeCase.LOGE)){
                if(inRelevantCall){
                    out.add(evt);
                }
            }else{
                out.add(evt);
            }

        }
        return out;
    }
    public void expandToDirectory(File f) throws IOException {
        //TODO: detect callin by finding calls where calling object matches app glob and calle does not
        if(!f.isDirectory()){
            throw new IllegalArgumentException("Must take dir");
        }
        Set<CallbackOuterClass.PValue> involvedobjectsset = getInvolvedObjects();
        List<CallbackOuterClass.PValue> involvedObjects = new ArrayList<>();
        for(CallbackOuterClass.PValue involvedObj : involvedobjectsset){
            involvedObjects.add(involvedObj);
        }
        //Write objects file
        String absolutePath = f.getAbsolutePath();
        String name = absolutePath + "/ObjectsList.proto";
        FileOutputStream fo = new FileOutputStream(name);
        for(Object o : involvedObjects){
            CallbackOuterClass.PValue ob = (CallbackOuterClass.PValue) o;
            ob.writeDelimitedTo(fo);
        }
        fo.close();
        name = absolutePath + "/ObjectList.txt";
        FileWriter fw = new FileWriter(name);
        int count = 0;
        for(Object o : involvedObjects){
            fw.append(Integer.toString(count));
            fw.append("\n");
            fw.append(o.toString());
            ++count;
        }
        fw.close();

        for(int i = 0; i< involvedObjects.size(); ++i){
            CallbackOuterClass.PValue involvedObject = involvedObjects.get(i);
            MObject mObject = MObject.fromPValue(involvedObject);
            List<CallbackOuterClass.EventInCallback> eventsu = objects.get(mObject);
            List<CallbackOuterClass.EventInCallback> events = filterExceptionsFromEvents(eventsu);
            String s = Integer.toString(i);
            FileOutputStream fos = new FileOutputStream(absolutePath + "/" + s + ".proto");
            FileWriter fwr = new FileWriter(absolutePath + "/" + s + ".txt");
            FileWriter humanfwr = new FileWriter(absolutePath + "/" + s + "-human.txt");
            for (CallbackOuterClass.EventInCallback event : events) {
                event.writeDelimitedTo(fos);
                fwr.append(event.toString());
            }
            List<DPEvent> dpEvents = nestedTraces.get(mObject);
            for (DPEvent event: dpEvents){
                if(event.callbacks.size() != 0 || event.events.size() !=0) {
                    humanfwr.append("=============================================================================\n");
                    if (event.eventInCallback != null) {
                        humanfwr.append(event.eventInCallback.toString());
                    } else {
                        humanfwr.append("Initial event\n");
                    }
                    humanfwr.append("--Callbacks--\n");
                    for (DPCallback dpCallback : event.callbacks) {
                        humanfwr.append(dpCallback.getMethodEvent().toString());
                    }
                    humanfwr.append("--Events--\n");
                    for (DPCallbackEvent dpEvent : event.events) {
                        if (dpEvent instanceof DPCallin) {
                            //TODO: fix exception event thing
                            humanfwr.append("-----Callin");
                            humanfwr.append(dpEvent.toString());
                        }else{
                            humanfwr.append("-----Exception");
                            humanfwr.append(dpEvent.toString());

                        }
                    }
                }

            }
            fos.close();
            fwr.close();
            humanfwr.close();

        }

        //Write

    }
    private Map<MObject,List<DPEvent>> nestedTraces = new HashMap<>();
    public void generateNestedTraces(){
        Set<MObject> involvedObjects = objects.keySet();
        for(MObject mObject : involvedObjects){
            List<CallbackOuterClass.EventInCallback> events = objects.get(mObject);
            List<DPEvent> dpEvents = new ArrayList<>();
            DPEvent currentEvent = new DPEvent(); //first event is all method invocations that come before
            boolean inCallin = false;
            for(CallbackOuterClass.EventInCallback event : events){
                if(event.getEventTypeCase().equals(CallbackOuterClass.EventInCallback.EventTypeCase.CALLBACK)){
                    dpEvents.add(currentEvent);
                    currentEvent = new DPEvent();
                    currentEvent.eventInCallback = event;
                    inCallin = false;
                }else if(event.getEventTypeCase().equals(CallbackOuterClass.EventInCallback.EventTypeCase.METHODEVENT)){
                    CallbackOuterClass.MethodEvent methodEvent = event.getMethodEvent();
                    if(methodEvent.getEventType().equals(CallbackOuterClass.EventType.METHODENTRY)){
                        if(methodEvent.getIsCallback()){
                            currentEvent.callbacks.add(new DPCallback(methodEvent));
                        }else{
                            if(!(isInClass(appPackageGlob, methodEvent)) &&
                                    isCallerInClass(appPackageGlob, methodEvent)){
                                currentEvent.events.add(new DPCallin(methodEvent));
                                inCallin = true;
                            }
                        }
                    }else if(methodEvent.getEventType().equals(CallbackOuterClass.EventType.METHODEXIT)){
                        if(!(isInClass(appPackageGlob, methodEvent))) {
                            inCallin = false;
                        }
                    }else{
                        //This should never happen
                        throw new IllegalStateException("method event doesn't have event type");
                    }
                }else if(event.getEventTypeCase()
                        .equals(CallbackOuterClass.EventInCallback.EventTypeCase.EXCEPTIONEVENT)){
                    //TODO: tack this onto the last method if in callin
                    if(inCallin) {
                        DPException exceptionEvent = new DPException(event.getExceptionEvent());
                        currentEvent.events.add(exceptionEvent);
                    }
                }
            }
            dpEvents.add(currentEvent);
        nestedTraces.put(mObject, dpEvents);
        }
    }
    public static boolean isInClass(String clazzGlob, CallbackOuterClass.MethodEvent methodEvent){
        String declaringType = methodEvent.getDeclaringType();
        return GlobUtil.matchGlob(clazzGlob, declaringType.split(" ")[1]);
    }
    public static boolean isCallerInClass(String clazzGlob, CallbackOuterClass.MethodEvent methodEvent){
        CallbackOuterClass.PValue caller = methodEvent.getCaller();
        if(caller.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.POBJCTREFERENC)){
            CallbackOuterClass.PObjectReference pValue = caller.getPObjctReferenc();
            boolean b = GlobUtil.matchGlob(clazzGlob, pValue.getType().split(" ")[1]);
            return b;
        }else{
            return false;
        }
    }
    public static class DPEvent{
        public CallbackOuterClass.EventInCallback eventInCallback;
        public List<DPCallback> callbacks = new ArrayList<>();
        public List<DPCallbackEvent> events = new ArrayList<>();
    }

    private static class DPCallback{
        private CallbackOuterClass.MethodEvent methodEvent;
        DPCallback(CallbackOuterClass.MethodEvent methodEvent){
            this.methodEvent = methodEvent;
        }
        public CallbackOuterClass.MethodEvent getMethodEvent(){
            return methodEvent;
        }
    }
    private static class DPCallin extends DPCallbackEvent{
        private final CallbackOuterClass.MethodEvent methodEvent;

        DPCallin(CallbackOuterClass.MethodEvent methodEvent){
            this.methodEvent = methodEvent;
        }

        public CallbackOuterClass.MethodEvent getMethodEvent() {
            return methodEvent;
        }

        @Override
        public String toString() {
            return methodEvent.toString();
        }
    }
    public static class DPException extends DPCallbackEvent{
        private final CallbackOuterClass.ExceptionEvent exceptionEvent;

        public DPException(CallbackOuterClass.ExceptionEvent exceptionEvent) {
            this.exceptionEvent = exceptionEvent;
        }

        public CallbackOuterClass.ExceptionEvent getExceptionEvent() {
            return exceptionEvent;
        }

        @Override
        public String toString() {
            return exceptionEvent.toString();
        }
    }

    abstract private static class DPCallbackEvent {
        public abstract String toString();
    }
    public static JSONObject mObjectToJson(MObject mObject){
        JSONObject obj = new JSONObject();
        if(mObject.pValue.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.POBJCTREFERENC)){
            CallbackOuterClass.PObjectReference pobj = mObject.getpValue().getPObjctReferenc();
            obj.put("type", pobj.getType());
        }else{
            obj.put("other", "NonObjRef");
        }
        return obj;
    }
    public static JSONObject eventToJson(DPEvent dpEvent){

        JSONObject obj = new JSONObject();
        LinkedList<JSONObject> callbacks = new LinkedList<>();

        for(DPCallback cb : dpEvent.callbacks){
            JSONObject jcb = new JSONObject();
            jcb.put("method", cb.getMethodEvent().getFullname());
            jcb.put("declaringType", cb.getMethodEvent().getDeclaringType());
            callbacks.add(jcb);
        }

        LinkedList<JSONObject> events = new LinkedList<>();
        for (DPCallbackEvent event : dpEvent.events) {
            JSONObject jev = new JSONObject();
            if(event instanceof DPCallin){
                DPCallin callinEvent = (DPCallin) event;
                jev.put("eventtype", "callin");
                jev.put("name", callinEvent.getMethodEvent().getFullname());
                jev.put("type", callinEvent.getMethodEvent().getDeclaringType());
            }else{
                DPException exceptionEvent = (DPException) event;
                jev.put("eventtype", "exception");
                jev.put("exception", exceptionEvent.getExceptionEvent().getException().getPObjctReferenc().getType());

            }
            events.add(jev);
        }
        obj.put("callbacks", callbacks);
        //TODO: what target and callback fields
        CallbackOuterClass.Callback callback = dpEvent.eventInCallback.getCallback();
        obj.put("what", callback.getWhat());
        obj.put("callbackField", callback.getCallback().getPObjctReferenc().getType());
        obj.put("targetField", callback.getTarget().getPObjctReferenc().getType());
        
        return obj;
    }
    public void writeJsonObject(FileWriter fileWriter) throws IOException {
        JSONObject objectsInClass = new JSONObject();
        LinkedHashMap objectTraces = new LinkedHashMap();
        Set<MObject> mObjects = nestedTraces.keySet();
        JSONObject currentObject;
        for(MObject mObject : mObjects){
            currentObject = mObjectToJson(mObject);
            List<JSONObject> events = new ArrayList<>();
            List<DPEvent> dpEvents = nestedTraces.get(mObject);
            for(DPEvent event : dpEvents){
                events.add(eventToJson(event));

            }
            objectTraces.put(currentObject, dpEvents);
        }
        objectsInClass.put("ObjectTraces", objectTraces);
        fileWriter.append(objectsInClass.toJSONString());

    }
}
