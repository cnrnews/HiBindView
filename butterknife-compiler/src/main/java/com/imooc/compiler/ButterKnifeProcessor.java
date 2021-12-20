package com.imooc.compiler;

import com.google.auto.service.AutoService;
import com.imooc.annotations.BindView;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

@AutoService(Processor.class)
public class ButterKnifeProcessor extends AbstractProcessor {

    private Filer mFilter;
    private Elements  mElementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {

        System.out.println("init---------------");

        super.init(processingEnvironment);
        mFilter = processingEnvironment.getFiler();
        processingEnvironment.getElementUtils();
    }

    /**
     * 获取版本号
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 添加需要的注解
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Class<? extends Annotation> annotation : getSupportedAnnotations()) {
            types.add(annotation.getCanonicalName());
        }
        return types;
    }

    private Set<Class<? extends Annotation>> getSupportedAnnotations(){
        Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();
        annotations.add(BindView.class);
        return annotations;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        System.out.println("-------------------------");
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(BindView.class);
        // 解析属性 activity -> List<Element>
        LinkedHashMap<Element, List<Element>> elementMap = new LinkedHashMap<>();
        for (Element element : elements) {

            // 获取注解的Activity类
            Element enclosingElement = element.getEnclosingElement();
            List<Element> viewBindElements  = elementMap.get(enclosingElement);
            if (viewBindElements==null){
                viewBindElements = new ArrayList<>();
                elementMap.put(enclosingElement,viewBindElements);
            }
            // 添加对应Activity类的 注解View集合
            viewBindElements.add(element);

        }

        // 生成代码
        for (Map.Entry<Element, List<Element>> entry : elementMap.entrySet()) {

            Element enclosingElements = entry.getKey();
            List<Element> viewBindElements = entry.getValue();

            // public final public xxxActivity implements Unbinder
            // 生成类声明
            String activityClassNameStr = enclosingElements.getSimpleName().toString();
            ClassName activityClassName = ClassName.bestGuess(activityClassNameStr);
            ClassName unbinderClassName = ClassName.get("com.imooc.butterknife", "Unbinder");
            TypeSpec.Builder classBuilder = TypeSpec.classBuilder(activityClassNameStr + "_ViewBinding")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addSuperinterface(unbinderClassName)
                    .addField(activityClassName,"target", Modifier.PRIVATE);// 添加private 类属性

            // 实现unbind 方法
            ClassName callSuperClassName = ClassName.get("androidx.annotation", "CallSuper");
            MethodSpec.Builder unbindMethodBuilder = MethodSpec.methodBuilder("unbind");
            unbindMethodBuilder.addAnnotation(Override.class);
            unbindMethodBuilder.addAnnotation(callSuperClassName);
            unbindMethodBuilder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);

            // 反注册
            unbindMethodBuilder.addStatement("$T target = this.target",activityClassName);
            unbindMethodBuilder.addStatement("if(target == null) throw  new IllegalArgumentException(\"Bindings already cleared!\");");


            // 构造函数
            MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                    .addParameter(activityClassName, "target");
            // this.target = target
            constructorBuilder.addStatement("this.target = target");

            // 注入属性
            for (Element viewBindElement : viewBindElements) {

                // target.textView1 = Utils.findViewById(source,R.id.tv1)
                String filedName = viewBindElement.getSimpleName().toString();
                ClassName utilsClassName = ClassName.get("com.imooc.butterknife", "Utils");
                int resId = viewBindElement.getAnnotation(BindView.class).value();
                constructorBuilder.addStatement("target.$L = $T.findViewById(target,$L)",
                        filedName,utilsClassName,resId);
                // target.textView1 = null
                unbindMethodBuilder.addStatement("target.$L = null",filedName);
            }


            classBuilder.addMethod(unbindMethodBuilder.build());
            classBuilder.addMethod(constructorBuilder.build());

            // 生成类
            String packageName = mElementUtils.getPackageOf(enclosingElements).getQualifiedName().toString();
            try {
                JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build())
                        .addFileComment("butterknife 自动生成")
                        .build();
                javaFile.writeTo(mFilter);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }
}