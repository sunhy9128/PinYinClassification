import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import com.xiaoniu.core.support.AppMessage;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.*;
 public class ClassifyByParam {
        public ClassifyByParam() {
        }

        public <T> AppMessage classifyByFirstLetter(List<T> list, String field, String... args) {
            try {
                Class<?> aClass = list.get(0).getClass();
                String ffield = field.substring(0, 1).toUpperCase() + field.substring(1);
                Method classDeclaredMethod = aClass.getDeclaredMethod("get" + ffield);
                String[] alphaTable = new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
                List<T> tList = this.sortList(list, field);
                if (tList == null) {
                    return AppMessage.error("1", new Object[0]);
                }

                if (tList.isEmpty()) {
                    return AppMessage.successdata("0", "", new Object[0]);
                }

                JSONArray jsonArray = new JSONArray();
                String[] var10 = alphaTable;
                int var11 = alphaTable.length;

                for(int var12 = 0; var12 < var11; ++var12) {
                    String firstLetter = var10[var12];
                    JSONObject object = new JSONObject();
                    List<JSONObject> jsonObjects = new ArrayList();

                    for(int i = 0; i < tList.size(); ++i) {
                        if (this.toPinyin(classDeclaredMethod.invoke(tList.get(i)).toString().substring(0, 1).toUpperCase()).equals(firstLetter)) {
                            JSONObject jsonObject = new JSONObject();

                            for(int j = 0; j < args.length; ++j) {
                                String fieldName = args[j];
                                String getMethodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                                List<String> fieldNames = new ArrayList();
                                Field[] var22 = aClass.getDeclaredFields();
                                int var23 = var22.length;

                                for(int var24 = 0; var24 < var23; ++var24) {
                                    Field field1 = var22[var24];
                                    fieldNames.add(field1.getName());
                                }

                                if (!fieldNames.contains(fieldName)) {
                                    String fieldValue = aClass.getSuperclass().getDeclaredMethod(getMethodName).invoke(tList.get(i)).toString();
                                    jsonObject.put(fieldName, fieldValue);
                                } else {
                                    Method fieldMethod = aClass.getDeclaredMethod(getMethodName);
                                    String fieldValue = fieldMethod.invoke(tList.get(i)).toString();
                                    jsonObject.put(fieldName, fieldValue);
                                }
                            }

                            jsonObjects.add(jsonObject);
                        }
                    }

                    if (jsonObjects.size() > 0) {
                        object.put("firstLetter", firstLetter);
                        object.put("list", jsonObjects);
                        jsonArray.add(object);
                    }
                }
                return AppMessage.successdata("0", jsonArray, new Object[0]);
            } catch (NoSuchMethodException var26) {
                var26.printStackTrace();
            } catch (IllegalAccessException var27) {
                var27.printStackTrace();
            } catch (InvocationTargetException var28) {
                var28.printStackTrace();
            }

            return AppMessage.error("2", new Object[0]);
        }

        private String toPinyin(String originHanCharacter) {
            char[] chars = originHanCharacter.trim().replaceAll(" ", "").toCharArray();
            String pinyin = "";

            for(int i = 0; i < chars.length; ++i) {
                if (chars[i] > 128) {
                    try {
                        pinyin = pinyin + PinyinHelper.getShortPinyin(chars[i] + "").toUpperCase();
                    } catch (PinyinException var6) {
                        var6.printStackTrace();
                    }
                } else {
                    pinyin = pinyin + chars[i];
                }
            }

            return pinyin;
        }

        private <T> List<T> sortList(List<T> list, String field) {
            try {
                if (list == null) {
                    return null;
                } else if (list.isEmpty()) {
                    return new ArrayList();
                } else {
                    Class<?> aClass = list.get(0).getClass();
                    String ffield = field.substring(0, 1).toUpperCase() + field.substring(1);
                    final Method classDeclaredMethod = aClass.getDeclaredMethod("get" + ffield);
                    Collections.sort(list, new Comparator<T>() {
                        public int compare(T o1, T o2) {
                            Method finalMethod = classDeclaredMethod;
                            Collator collator = Collator.getInstance(Locale.CHINA);

                            try {
                                return collator.compare(finalMethod.invoke(o1), finalMethod.invoke(o2));
                            } catch (IllegalAccessException var6) {
                                var6.printStackTrace();
                            } catch (InvocationTargetException var7) {
                                var7.printStackTrace();
                            }

                            return -1;
                        }
                    });
                    return list;
                }
            } catch (NoSuchMethodException var6) {
                var6.printStackTrace();
                return new ArrayList();
            }
        }
    }