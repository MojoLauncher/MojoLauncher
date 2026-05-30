package dalvik.annotation.optimization

//  Dummy CriticalNative annotation. On devices which dont have it this declaration
//  On devices that do have it it will be overridden by the system one and work as usual
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class CriticalNative 
