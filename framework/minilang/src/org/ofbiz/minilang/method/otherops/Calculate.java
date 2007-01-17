/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.minilang.method.otherops;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Calculates a result based on nested calcops.
 */
public class Calculate extends MethodOperation {
    
    public static final String module = Calculate.class.getName();
    
    public static final BigDecimal ZERO = new BigDecimal(0.0);
    public static final int TYPE_DOUBLE = 1;
    public static final int TYPE_FLOAT = 2;
    public static final int TYPE_LONG = 3;
    public static final int TYPE_INTEGER = 4;
    public static final int TYPE_STRING = 5;
    public static final int TYPE_BIG_DECIMAL = 6;

    ContextAccessor mapAcsr;
    ContextAccessor fieldAcsr;
    String decimalScaleString;
    String decimalFormatString;
    String typeString;
    String roundingModeString;
    Calculate.SubCalc calcops[];

    public Calculate(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));

        decimalScaleString = element.getAttribute("decimal-scale");
        decimalFormatString = element.getAttribute("decimal-format");
        typeString = element.getAttribute("type");
        roundingModeString = element.getAttribute("rounding-mode");

        List calcopElements = UtilXml.childElementList(element);
        calcops = new Calculate.SubCalc[calcopElements.size()];
        Iterator calcopIter = calcopElements.iterator();
        int i = 0;

        while (calcopIter.hasNext()) {
            Element calcopElement = (Element) calcopIter.next();
            String nodeName = calcopElement.getNodeName();

            if ("calcop".equals(nodeName)) {
                calcops[i] = new Calculate.CalcOp(calcopElement);
            } else if ("number".equals(nodeName)) {
                calcops[i] = new Calculate.NumberOp(calcopElement);
            } else {
                Debug.logError("Error: calculate operation with type " + nodeName, module);
            }
            // Debug.logInfo("Added operation type " + nodeName + " in position " + i, module);
            i++;
        }
    }

    public boolean exec(MethodContext methodContext) {
        String typeString = methodContext.expandString(this.typeString);
        int type;
        if ("Double".equals(typeString)) {
            type = Calculate.TYPE_DOUBLE;
        } else if ("Float".equals(typeString)) {
            type = Calculate.TYPE_FLOAT;
        } else if ("Long".equals(typeString)) {
            type = Calculate.TYPE_LONG;
        } else if ("Integer".equals(typeString)) {
            type = Calculate.TYPE_INTEGER;
        } else if ("String".equals(typeString)) {
            type = Calculate.TYPE_STRING;
        } else if ("BigDecimal".equals(typeString)) {
            type = Calculate.TYPE_BIG_DECIMAL;
        } else {
            type = Calculate.TYPE_DOUBLE;
        }
        
        String roundingModeString = methodContext.expandString(this.roundingModeString);
        int roundingMode;
        if ("Ceiling".equals(roundingModeString)) {
            roundingMode = BigDecimal.ROUND_CEILING;
        } else if ("Floor".equals(roundingModeString)) {
            roundingMode = BigDecimal.ROUND_FLOOR;
        } else if ("Up".equals(roundingModeString)) {
            roundingMode = BigDecimal.ROUND_UP;
        } else if ("Down".equals(roundingModeString)) {
            roundingMode = BigDecimal.ROUND_DOWN;
        } else if ("HalfUp".equals(roundingModeString)) {
            roundingMode = BigDecimal.ROUND_HALF_UP;
        } else if ("HalfDown".equals(roundingModeString)) {
            roundingMode = BigDecimal.ROUND_HALF_DOWN;
        } else if ("HalfEven".equals(roundingModeString)) {
            roundingMode = BigDecimal.ROUND_HALF_EVEN;
        } else if ("Unnecessary".equals(roundingModeString)) {
            roundingMode = BigDecimal.ROUND_UNNECESSARY;
        } else {
            // default to HalfEven, reduce cumulative errors
            roundingMode = BigDecimal.ROUND_HALF_EVEN;
        }

        String decimalScaleString = methodContext.expandString(this.decimalScaleString);
        int decimalScale = 2;
        if (UtilValidate.isNotEmpty(decimalScaleString)) {
            decimalScale = Integer.valueOf(decimalScaleString).intValue();
        }
        
        String decimalFormatString = methodContext.expandString(this.decimalFormatString);
        DecimalFormat df = null;
        if (UtilValidate.isNotEmpty(decimalFormatString)) {
            df = new DecimalFormat(decimalFormatString);
        }
        
        BigDecimal resultValue = ZERO;
        resultValue = resultValue.setScale(decimalScale, roundingMode);
        for (int i = 0; i < calcops.length; i++) {
            resultValue = resultValue.add(calcops[i].calcValue(methodContext, decimalScale, roundingMode));
            // Debug.logInfo("main total so far: " + resultValue, module);
        }
        resultValue = resultValue.setScale(decimalScale, roundingMode);
        
        /* the old thing that did conversion to string and back, may want to use somewhere sometime...:
         * for now just doing the setScale above (before and after calc ops)
        try {
            resultValue = new BigDecimal(df.format(resultValue));
        } catch (ParseException e) {
            String errorMessage = "Unable to format [" + formatString + "] result [" + resultValue + "]";
            Debug.logError(e, errorMessage, module);
            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventErrorMessageName(), errorMessage);
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageName(), errorMessage);
            }
            return false;
        }
        */
        
        Object resultObj = null;
        switch (type) {
        case TYPE_DOUBLE:
            resultObj = new Double(resultValue.doubleValue());
            break;
        case TYPE_FLOAT:
            resultObj = new Float(resultValue.floatValue());
            break;
        case TYPE_LONG:
            resultValue = resultValue.setScale(0, roundingMode);
            resultObj = new Long(resultValue.longValue());
            break;
        case TYPE_INTEGER:
            resultValue = resultValue.setScale(0, roundingMode);
            resultObj = new Integer(resultValue.intValue());
            break;
        case TYPE_STRING:
            // run the decimal-formatting         
            if (df != null && resultValue.compareTo(ZERO) > 0) {
                resultObj = df.format(resultValue);
            } else {
                resultObj = resultValue.toString();
            }
            break;            
        case TYPE_BIG_DECIMAL:
            resultObj = resultValue;
            break;            
        }

        if (!mapAcsr.isEmpty()) {
            Map toMap = (Map) mapAcsr.get(methodContext);
            if (toMap == null) {
                if (Debug.verboseOn()) Debug.logVerbose("Map not found with name " + mapAcsr + ", creating new map", module);
                toMap = new HashMap();
                mapAcsr.put(methodContext, toMap);
            }
            fieldAcsr.put(toMap, resultObj, methodContext);
        } else {
            fieldAcsr.put(methodContext, resultObj);
        }

        return true;
    }

    public String rawString() {
        // TODO: add all attributes and other info
        return "<calculate field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }

    protected static interface SubCalc {
        public BigDecimal calcValue(MethodContext methodContext, int scale, int roundingMode);
    }

    protected static class NumberOp implements SubCalc {
        String valueStr;

        public NumberOp(Element element) {
            valueStr = element.getAttribute("value");
        }

        public BigDecimal calcValue(MethodContext methodContext, int scale, int roundingMode) {
            String valueStr = methodContext.expandString(this.valueStr);
            BigDecimal value;
            try {
                value = new BigDecimal(valueStr);
                value = value.setScale(scale, roundingMode);
            } catch (Exception e) {
                Debug.logError(e, "Could not parse the number string: " + valueStr, module);
                throw new IllegalArgumentException("Could not parse the number string: " + valueStr);
            }
            
            // Debug.logInfo("calcValue number: " + value, module);
            return value;
        }

    }

    protected static class CalcOp implements SubCalc {
        public static final int OPERATOR_ADD = 1;
        public static final int OPERATOR_SUBTRACT = 2;
        public static final int OPERATOR_MULTIPLY = 3;
        public static final int OPERATOR_DIVIDE = 4;
        public static final int OPERATOR_NEGATIVE = 5;

        ContextAccessor mapAcsr;
        ContextAccessor fieldAcsr;
        String operatorStr;
        Calculate.SubCalc calcops[];

        public CalcOp(Element element) {
            mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
            fieldAcsr = new ContextAccessor(element.getAttribute("field-name"));
            operatorStr = element.getAttribute("operator");

            List calcopElements = UtilXml.childElementList(element);
            calcops = new Calculate.SubCalc[calcopElements.size()];
            Iterator calcopIter = calcopElements.iterator();
            int i = 0;

            while (calcopIter.hasNext()) {
                Element calcopElement = (Element) calcopIter.next();
                String nodeName = calcopElement.getNodeName();

                if ("calcop".equals(calcopElement.getNodeName())) {
                    calcops[i] = new Calculate.CalcOp(calcopElement);
                } else if ("number".equals(calcopElement.getNodeName())) {
                    calcops[i] = new Calculate.NumberOp(calcopElement);
                } else {
                    Debug.logError("Error: calculate operation unknown with type " + nodeName, module);
                }
                // Debug.logInfo("Added operation type " + nodeName + " in position " + i, module);
                i++;
            }
        }

        public BigDecimal calcValue(MethodContext methodContext, int scale, int roundingMode) {
            String operatorStr = methodContext.expandString(this.operatorStr);
            int operator = CalcOp.OPERATOR_ADD;
            if ("get".equals(operatorStr)) {
                operator = CalcOp.OPERATOR_ADD;
            } else if ("add".equals(operatorStr)) {
                operator = CalcOp.OPERATOR_ADD;
            } else if ("subtract".equals(operatorStr)) {
                operator = CalcOp.OPERATOR_SUBTRACT;
            } else if ("multiply".equals(operatorStr)) {
                operator = CalcOp.OPERATOR_MULTIPLY;
            } else if ("divide".equals(operatorStr)) {
                operator = CalcOp.OPERATOR_DIVIDE;
            } else if ("negative".equals(operatorStr)) {
                operator = CalcOp.OPERATOR_NEGATIVE;
            }
            
            BigDecimal resultValue = ZERO;
            resultValue = resultValue.setScale(scale, roundingMode);
            boolean isFirst = true;

            // if a fieldAcsr was specified, get the field from the map or result and use it as the initial value
            if (!fieldAcsr.isEmpty()) {
                Object fieldObj = null;

                if (!mapAcsr.isEmpty()) {
                    Map fromMap = (Map) mapAcsr.get(methodContext);
                    if (fromMap == null) {
                        if (Debug.verboseOn()) Debug.logVerbose("Map not found with name " + mapAcsr + ", creating new map", module);
                        fromMap = new HashMap();
                        mapAcsr.put(methodContext, fromMap);
                    }
                    fieldObj = fieldAcsr.get(fromMap, methodContext);
                } else {
                    fieldObj = fieldAcsr.get(methodContext);
                }

                if (fieldObj != null) {
                    if (fieldObj instanceof Double) {
                        resultValue = new BigDecimal(((Double) fieldObj).doubleValue());
                    } else if (fieldObj instanceof Long) {
                        resultValue = BigDecimal.valueOf(((Long) fieldObj).longValue());
                    } else if (fieldObj instanceof Float) {
                        resultValue = new BigDecimal(((Float) fieldObj).floatValue());
                    } else if (fieldObj instanceof Integer) {
                        resultValue = BigDecimal.valueOf(((Integer) fieldObj).longValue());
                    } else if (fieldObj instanceof String) {
                        resultValue = new BigDecimal((String) fieldObj);
                    } else if (fieldObj instanceof BigDecimal) {
                        resultValue = (BigDecimal) fieldObj;
                    }
                    if (operator == OPERATOR_NEGATIVE) resultValue = resultValue.negate();
                    isFirst = false;
                } else {
                    if (Debug.infoOn()) Debug.logInfo("Field not found with field-name " + fieldAcsr + ", and map-name " + mapAcsr + "using a default of 0", module);
                }
            }

            for (int i = 0; i < calcops.length; i++) {
                if (isFirst) {
                    resultValue = calcops[i].calcValue(methodContext, scale, roundingMode);
                    if (operator == OPERATOR_NEGATIVE) resultValue = resultValue.negate();
                    isFirst = false;
                } else {
                    switch (operator) {
                    case OPERATOR_ADD:
                        resultValue = resultValue.add(calcops[i].calcValue(methodContext, scale, roundingMode));
                        break;
                    case OPERATOR_SUBTRACT:
                    case OPERATOR_NEGATIVE:
                        resultValue = resultValue.subtract(calcops[i].calcValue(methodContext, scale, roundingMode));
                        break;
                    case OPERATOR_MULTIPLY:
                        resultValue = resultValue.multiply(calcops[i].calcValue(methodContext, scale, roundingMode));
                        break;
                    case OPERATOR_DIVIDE:
                        resultValue = resultValue.divide(calcops[i].calcValue(methodContext, scale, roundingMode), scale, roundingMode);
                        break;
                    }
                }
                // Debug.logInfo("sub total so far: " + resultValue, module);
            }
            // Debug.logInfo("calcValue calcop: " + resultValue + "(field=" + fieldAcsr + ", map=" + mapAcsr + ")", module);
            return resultValue;
        }
    }
}
