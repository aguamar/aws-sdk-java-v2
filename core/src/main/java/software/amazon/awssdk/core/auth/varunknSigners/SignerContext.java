package software.amazon.awssdk.core.auth.varunknSigners;


import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;

public class SignerContext {

    private final Map<ExecutionAttribute<?>, Object> attributes = new HashMap<>();

    /**
     * Retrieve the current value of the provided attribute in this collection of attributes. This will return null if the value
     * is not set.
     */
    @SuppressWarnings("unchecked") // Cast is safe due to implementation of {@link #putAttribute}
    public <U> U getAttribute(ExecutionAttribute<U> attribute) {
        return (U) attributes.get(attribute);
    }

    /**
     * Update or set the provided attribute in this collection of attributes.
     */
    public <U> SignerContext putAttribute(ExecutionAttribute<U> attribute, U value) {
        this.attributes.put(attribute, value);
        return this;
    }

}
