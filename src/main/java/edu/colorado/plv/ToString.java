package edu.colorado.plv;

/**
 * Created by s on 6/17/16.
 */
public class ToString {
    public static String to_str(CallbackOuterClass.PObjectReference pObjectReference){
        String out;
        if(pObjectReference.hasId()) {
            out = "pObj_";
            out += pObjectReference.getType();
            out += "@";
            out += pObjectReference.getId();
        }else{
            out = "pObj_primitive";
            //perhaps add prim values here if ever needed
        }
        return out;
    }
    public static String to_str(CallbackOuterClass.ConcreteObject concreteObject){
        String out = "obj_";
        out += concreteObject.getId();
        out += "@";
        out += concreteObject.getType();
        return out;
    }
    public static String to_str(CallbackOuterClass.PValue pValue){
        if(pValue.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.POBJCTREFERENC)) {
            return to_str(pValue.getPObjctReferenc());
        }else if(pValue.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.PNULL)) {
            return "null";
        }else if(pValue.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.PINTEGERVALUE)){
            return "integer_v";
        }else if(pValue.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.PBOOLVALUE)){
            return Boolean.toString(pValue.getPBoolValue());
        }else if(pValue.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.PFLOATVALUE)){
            return "float_v";
        }else if(pValue.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.POTHERVALUE)){
            return "other_v";
        }else if(pValue.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.PSTRINGREFERENCE)){
            return "string_v";
        }else {
            throw new UnsupportedOperationException();
        }
    }
    public static boolean isNonExtendingPval(CallbackOuterClass.PValue pValue){
        if(pValue.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.POBJCTREFERENC)) {
            CallbackOuterClass.PObjectReference pObjctReferenc = pValue.getPObjctReferenc();
            if(pObjctReferenc.getFirstFrameworkSuper().equals("")){
                return pObjctReferenc.getInterfacesCount()==0;
            }
            return false;
        }else if(pValue.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.PSTRINGREFERENCE)){
            return false;
        }else if(pValue.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.POTHERVALUE)) {
            return false;
        }else if(pValue.getValueTypeCase().equals(CallbackOuterClass.PValue.ValueTypeCase.PNULL)){
            return false;
        }else {
            throw new UnsupportedOperationException();
        }
    }
}
