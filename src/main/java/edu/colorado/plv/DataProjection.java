package edu.colorado.plv;


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
        if(!f.isDirectory()){
            throw new IllegalArgumentException("Must take dir");
        }
        Set<CallbackOuterClass.PValue> involvedobjectsset = getInvolvedObjects();
        List<CallbackOuterClass.PValue> involvedObjects = new ArrayList<>();
        for(CallbackOuterClass.PValue involvedObj : involvedobjectsset){
            involvedObjects.add(involvedObj);
        }
        //Write objects file (TODO)
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
            List<CallbackOuterClass.EventInCallback> eventsu = objects.get(MObject.fromPValue(involvedObject));
            List<CallbackOuterClass.EventInCallback> events = filterExceptionsFromEvents(eventsu);
            String s = Integer.toString(i);
            FileOutputStream fos = new FileOutputStream(absolutePath + "/" + s + ".proto");
            FileWriter fwr = new FileWriter(absolutePath + "/" + s + ".txt");
            for (CallbackOuterClass.EventInCallback event : events) {
                event.writeDelimitedTo(fos);
                fwr.append(event.toString());
            }
            fos.close();
            fwr.close();

        }

        //Write

    }
    private Map<MObject,List<DPEvent>> nestedTraces = new HashMap<>();
    public void generateNestedTraces(){
        Set<MObject> involvedObjects = objects.keySet();
        for(MObject mObject : involvedObjects){
            List<CallbackOuterClass.EventInCallback> events = objects.get(mObject);
            int callCount = 0;
            List<DPEvent> dpEvents = new ArrayList<>();
            DPEvent currentEvent = new DPEvent(); //first event is all method invocations that come before
            for(CallbackOuterClass.EventInCallback event : events){
                if(event.getEventTypeCase().equals(CallbackOuterClass.EventInCallback.EventTypeCase.CALLBACK)){
                    dpEvents.add(currentEvent);
                    currentEvent = new DPEvent();
                    currentEvent.eventInCallback = event;
                    callCount = 0;
                }else if(event.getEventTypeCase().equals(CallbackOuterClass.EventInCallback.EventTypeCase.METHODEVENT)){
                    CallbackOuterClass.MethodEvent methodEvent = event.getMethodEvent();
                    if(methodEvent.getEventType().equals(CallbackOuterClass.EventType.METHODENTRY)){
                        if(methodEvent.getIsCallback()){
                            currentEvent.callbacks.add(new DPCallback(methodEvent));
                        }else{
                            if(!(isInClass(appPackageGlob, methodEvent))){
                                if(callCount == 0){
                                    currentEvent.events.add(new DPCallin(methodEvent));
                                }
                                ++callCount;
                            }
                        }
                    }else if(methodEvent.getEventType().equals(CallbackOuterClass.EventType.METHODEXIT)){
                        if(!(isInClass(appPackageGlob, methodEvent))) {
                            --callCount;
                        }
                    }else{
                        //This should never happen
                        throw new IllegalStateException("method event doesn't have event type");
                    }
                }else if(event.getEventTypeCase()
                        .equals(CallbackOuterClass.EventInCallback.EventTypeCase.EXCEPTIONEVENT)){
                    DPException exceptionEvent = new DPException(event.getExceptionEvent());
                    currentEvent.events.add(exceptionEvent);
                }
            }
        nestedTraces.put(mObject, dpEvents);
        }
    }
    public static boolean isInClass(String clazzGlob, CallbackOuterClass.MethodEvent methodEvent){
        String declaringType = methodEvent.getDeclaringType();
        return GlobUtil.matchGlob(clazzGlob, declaringType.split(" ")[1]);
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
    }
    public static class DPException extends DPCallbackEvent{
        private final CallbackOuterClass.ExceptionEvent exceptionEvent;

        public DPException(CallbackOuterClass.ExceptionEvent exceptionEvent) {
            this.exceptionEvent = exceptionEvent;
        }

        public CallbackOuterClass.ExceptionEvent getExceptionEvent() {
            return exceptionEvent;
        }
    }

    private static class DPCallbackEvent {}
}
