package edu.colorado.plv;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.*;

import static edu.colorado.plv.GlobUtil.matchGlob;

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
            if(vtc.equals(CallbackOuterClass.PValue.ValueTypeCase.POBJCTREFERENC)){
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
        System.out.println("generateNestedTraces");
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
        //TODO remove following print code
        Set<MObject> mObjects = nestedTraces.keySet();
        for (MObject mObject : mObjects) {
            int size = 0;
            List<DPEvent> dpEvents = nestedTraces.get(mObject);
            for (DPEvent dpEvent : dpEvents) {
                List<DPCallbackEvent> events = dpEvent.events;
                for (DPCallbackEvent event : events) {
                    if(event instanceof DPCallin){
                        size++;
                    }
                }

            }

            System.out.println("Trace size: " + size);
        }

    }
    public static boolean isInClass(String clazzGlob, CallbackOuterClass.MethodEvent methodEvent){
        String declaringType = methodEvent.getDeclaringType();
        return matchGlob(clazzGlob, declaringType.split(" ")[1]);
    }
    public static boolean isCallerInClass(String clazzGlob, CallbackOuterClass.MethodEvent methodEvent){
        CallbackOuterClass.PValue caller = methodEvent.getCaller();
        if(caller.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.POBJCTREFERENC)){
            CallbackOuterClass.PObjectReference pValue = caller.getPObjctReferenc();
            boolean b = matchGlob(clazzGlob, pValue.getType().split(" ")[1]);
            return b;
        }else{
            return false;
        }
    }

    /**
     * Event that runs through the event queue of Looper
     * contains the event info {what ...}
     * contains callbacks (first place for the control flow to pass to the application)
     */
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
            obj.put("framework_super", pobj.getFirstFrameworkSuper());
        }else{
            obj.put("other", "NonObjRef");
        }
        return obj;
    }
    public static boolean notInAppPackage(DPEvent dpEvent, String appPackageGlob){
        CallbackOuterClass.EventInCallback eventInCallback = dpEvent.eventInCallback;
        if(eventInCallback == null){
            return true;
        }
        CallbackOuterClass.Callback callback = eventInCallback.getCallback();
        CallbackOuterClass.PValue callback1 = callback.getCallback();
        if(callback1.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.POBJCTREFERENC)){
            CallbackOuterClass.PObjectReference pObjctReferenc = callback1.getPObjctReferenc();
            String type = pObjctReferenc.getType();
            if(matchGlob(appPackageGlob, type)){
                return false;
            }

        }
        CallbackOuterClass.PValue target = callback.getTarget();
        if(target.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.POBJCTREFERENC)){
            CallbackOuterClass.PObjectReference pObjctReferenc = target.getPObjctReferenc();
            String type = pObjctReferenc.getType();
            if(matchGlob(appPackageGlob, type)){
                return false;
            }
        }
        return true;

    }
    public static boolean isDataRelevantEvent(DPEvent dpEvent, MObject mObject){

        List<DPCallback> callbacks = dpEvent.callbacks;
        List<CallbackOuterClass.PValue> pvalues = new ArrayList<>();
        for (DPCallback callback : callbacks) {
            CallbackOuterClass.PValue calle = callback.methodEvent.getCalle();
            List<CallbackOuterClass.PValue> parametersList = callback.methodEvent.getParametersList();


            if(dpEvent.eventInCallback == null || dpEvent.eventInCallback.getCallback().getThreadID() == callback.getMethodEvent().getThreadID()) {
                pvalues.add(calle);
                pvalues.addAll(parametersList);
            }
        }

        for (CallbackOuterClass.PValue pvalue : pvalues) {
            //TODO: check if event is on same thread as callback
            if(pvalue.equals(mObject.pValue)){
                return true;
            }
        }
        return false;
    }
    public static List<JSONObject> eventToJson(DPEvent dpEvent, MObject mObject, String appPackageGlob){



        LinkedList<JSONObject> events = new LinkedList<>();

        JSONObject obj = new JSONObject();
        obj.put("eventtype", "event");
        if(dpEvent.eventInCallback != null) {
            CallbackOuterClass.Callback callback = dpEvent.eventInCallback.getCallback();
            obj.put("what", callback.getWhat());
            String type = callback.getCallback().getPObjctReferenc().getType();
            obj.put("callbackField", type);
            obj.put("targetField", callback.getTarget().getPObjctReferenc().getType());
        }else{
            obj.put("Message", "initial");
        }

        if(isDataRelevantEvent(dpEvent, mObject) && notInAppPackage(dpEvent, appPackageGlob)) {
            events.add(obj);
        }
        List<DPCallbackEvent> events1 = dpEvent.events;
        for (DPCallbackEvent event : events1) {
            JSONObject jev = new JSONObject();
            if(event instanceof DPCallin){
                DPCallin callinEvent = (DPCallin) event;
                jev.put("eventtype", "callin");
                CallbackOuterClass.MethodEvent methodEvent = callinEvent.getMethodEvent();
                jev = methodEventToJson(methodEvent);

            }else{
                DPException exceptionEvent = (DPException) event;
                jev.put("eventtype", "exception");
                jev.put("exception", exceptionEvent.getExceptionEvent().getException().getPObjctReferenc().getType());

            }
            events.add(jev);
        }
        //TODO: what target and callback fields


        return events;
    }

    public static JSONObject methodEventToJson(CallbackOuterClass.MethodEvent methodEvent) {
        JSONObject jev = new JSONObject();
        jev.put("name", methodEvent.getFullname());
        jev.put("type", methodEvent.getDeclaringType());
        jev.put("signature", methodEvent.getSignature());
        List<CallbackOuterClass.PValue> args = methodEvent.getParametersList();
        List<Boolean> boolArgs = new LinkedList<>();
        JSONArray cargs = new JSONArray();
        for (CallbackOuterClass.PValue arg : args) {
            CallbackOuterClass.PValue.ValueTypeCase valueTypeCase = arg.getValueTypeCase();
            if(valueTypeCase.equals(CallbackOuterClass.PValue.ValueTypeCase.PBOOLVALUE)){
                boolean pBoolValue = arg.getPBoolValue();
                boolArgs.add(pBoolValue);
            }
            cargs.add(ToString.to_str(arg));
        }
        jev.put("booleanArgs", boolArgs);
        jev.put("concreteArgs", cargs);



        return jev;
    }
    public static JSONObject methodEventToJson_short(CallbackOuterClass.MethodEvent methodEvent) {
        JSONObject jev = new JSONObject();
//        jev.put("name", methodEvent.getFullname());
//        String type = methodEvent.getDeclaringType();
        CallbackOuterClass.PValue calle = methodEvent.getCalle();
        List<CallbackOuterClass.PValue> args = methodEvent.getParametersList();
        String firstFrameworkSuper = null;
        if(calle.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.POBJCTREFERENC)) {
            firstFrameworkSuper = calle.getPObjctReferenc().getFirstFrameworkSuper();
            //jev.put("signature", firstFrameworkSuper);
        }else if(calle.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.PNULL)){
            firstFrameworkSuper = "";
            //throw new IllegalStateException("null is not callable");
        }else{
            throw new IllegalStateException("a primitive type is not callable, look for a bug");
        }

        JSONArray cargs = new JSONArray();
        cargs.add(ToString.to_str(calle)); //Add calle as first argument

        for (CallbackOuterClass.PValue arg : args) {
            cargs.add(ToString.to_str(arg));
        }
//        jev.put("booleanArgs", boolArgs);
        jev.put("concreteArgs", cargs);


        String signature = methodEvent.getSignature();
        String name = methodEvent.getFullname();
        String sig = firstFrameworkSuper+"."+name+signature;
        jev.put("signature", sig);
        return jev;
    }
    public static void methodEventToJson_short(JSONObject addTo, CallbackOuterClass.MethodEvent methodEvent) {
//        jev.put("name", methodEvent.getFullname());
//        String type = methodEvent.getDeclaringType();
        CallbackOuterClass.PValue calle = methodEvent.getCalle();
        List<CallbackOuterClass.PValue> args = methodEvent.getParametersList();
        String firstFrameworkSuper = null;
        if(calle.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.POBJCTREFERENC)) {
            firstFrameworkSuper = calle.getPObjctReferenc().getFirstFrameworkSuper();
            //jev.put("signature", firstFrameworkSuper);
        }else if(calle.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.PNULL)){
            firstFrameworkSuper = "";
            //throw new IllegalStateException("null is not callable");
        }else{
            throw new IllegalStateException("a primitive type is not callable, look for a bug");
        }

        JSONArray cargs = new JSONArray();
        cargs.add(ToString.to_str(calle)); //Add calle as first argument

        for (CallbackOuterClass.PValue arg : args) {
            cargs.add(ToString.to_str(arg));
        }
//        jev.put("booleanArgs", boolArgs);
        addTo.put("concreteArgs", cargs);


        String signature = methodEvent.getSignature();
        String name = methodEvent.getFullname();
        String sig = firstFrameworkSuper+"."+name+signature;
        addTo.put("signature", sig);
    }

    public void writeJsonObject(File file) throws IOException {

        Set<MObject> mObjects = nestedTraces.keySet();
        int count = 0;
        String parent = file.getPath();
        System.out.println("Output directory: " + parent);
        System.out.println("Number of Objects: " + mObjects.size());
        for(MObject mObject : mObjects) {
            JSONObject currentObject = mObjectToJson(mObject);

            List<JSONObject> events = new ArrayList<>();

            List<DPEvent> dpEvents = nestedTraces.get(mObject);

            if (containsCallins(dpEvents) && isObjectInReciever(dpEvents, mObject)) {
                for (DPEvent event : dpEvents) {
                    List<JSONObject> c = eventToJson(event, mObject, appPackageGlob);
                    events.addAll(c);
                }
                currentObject.put("events", events);

                String absolutePath = parent;
                String fileName = absolutePath +
                        File.separator +
                        Integer.toString(count) + ".json";
                System.out.println("File: " + fileName);
                FileWriter fileWriter = new FileWriter(fileName);
                currentObject.writeJSONString(fileWriter);
                fileWriter.close();
                ++count;
            }
        }

    }

    private boolean isObjectInReciever(List<DPEvent> dpEvents, MObject mObject) {
        boolean observedAsRec = false;
        CallbackOuterClass.PValue pValue = mObject.getpValue();
        if(pValue.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.POBJCTREFERENC)) {
            CallbackOuterClass.PObjectReference target = pValue.getPObjctReferenc();

            for (DPEvent dpEvent : dpEvents) {
                List<DPCallbackEvent> events = dpEvent.events;
                for (DPCallbackEvent event : events) {
                    if(event instanceof DPCallin){
                        DPCallin event1 = (DPCallin) event;
                        CallbackOuterClass.PValue calle = event1.getMethodEvent().getCalle();
                        CallbackOuterClass.PObjectReference pObjctReferenc = calle.getPObjctReferenc();
                        observedAsRec = observedAsRec || (target.equals(pObjctReferenc));
                    }
                }

//                if (eventInCallback.getEventTypeCase().equals(CallbackOuterClass.EventInCallback.EventTypeCase.METHODEVENT)) {
//                    CallbackOuterClass.MethodEvent methodEvent = eventInCallback.getMethodEvent();
//                    CallbackOuterClass.PValue calle = methodEvent.getCalle();
//                    observedAsRec = observedAsRec || (calle.equals(target));
//                }
            }
        }

        return observedAsRec;
    }

    public static boolean containsCallins(List<DPEvent> events){
        boolean res = false;
        for (DPEvent event : events) {
            res = res || event.events.size() > 0;
        }
        return res;
    }
}
