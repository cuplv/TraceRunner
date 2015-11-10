package edu.colorado.plv;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by s on 11/10/15.
 */
public class DataProjection {
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
    private Map<MObject, CallbackOuterClass.PObjectReference> objects = new HashMap<>();
    public static DataProjection fromFileInputStream(FileInputStream fileInputStream) {
        return null;
    }
}
