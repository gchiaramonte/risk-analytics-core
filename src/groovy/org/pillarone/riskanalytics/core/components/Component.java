package org.pillarone.riskanalytics.core.components;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.pillarone.riskanalytics.core.model.IModelVisitor;
import org.pillarone.riskanalytics.core.model.ModelPath;
import org.pillarone.riskanalytics.core.model.ModelPathComponent;
import org.pillarone.riskanalytics.core.packets.PacketList;
import org.pillarone.riskanalytics.core.parameterization.IParameterObject;
import org.pillarone.riskanalytics.core.simulation.SimulationException;
import org.pillarone.riskanalytics.core.simulation.engine.id.IIdGenerator;
import org.pillarone.riskanalytics.core.util.GroovyUtils;
import org.pillarone.riskanalytics.core.wiring.ITransmitter;

import java.lang.reflect.Field;
import java.util.*;

/**
 * todo: description of general concept
 */
abstract public class Component implements Cloneable {

    private String name;
    private List<ITransmitter> allInputTransmitter = new ArrayList<ITransmitter>();
    private List<ITransmitter> allOutputTransmitter = new ArrayList<ITransmitter>();
    private List<PacketList> inChannels = new ArrayList<PacketList>();
    private List<PacketList> outChannels = new ArrayList<PacketList>();
    private int transmitCount = 0;
    private IIdGenerator idGenerator;
    // todo(mwy): how to make sure that only classes derived of IComponentMarker are part of the list?
    private List<Class> markerList = new ArrayList<Class>();

    protected Component() {
        for (Class intf : this.getClass().getInterfaces()) {
            if (IComponentMarker.class.isAssignableFrom(intf)) {
                markerList.add(intf);
            }
        }
    }

    /** this constant is used for annotating PacketList */
    protected final static Integer N = Integer.MAX_VALUE;

    abstract protected void doCalculation();

    public void setIdGenerator(IIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public IIdGenerator getIdGenerator() {
        return idGenerator;
    }

    public List<ITransmitter> getAllInputTransmitter() {
        return allInputTransmitter;
    }

    public List<ITransmitter> getAllOutputTransmitter() {
        return allOutputTransmitter;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return ComponentUtils.getNormalizedName(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * To be overwritten if a special start behaviour is required. Per default execute is called.
     */

    public void start() {
        execute();
    }

    protected void execute() {
        try {
            doCalculation();
        }
        catch (SimulationException ex) {
            // extend the path with every recursive call of execute()
            ex.addPathElement(getName());
            throw ex;
        }
        publishResults();
        reset();
    }

    public void accept(IModelVisitor visitor, ModelPath path) {
        visitor.visitComponent(this, path);
        for (Map.Entry<String, Object> property : GroovyUtils.getProperties(this).entrySet()) {
            Object propertyValue = property.getValue();
            if (property.getKey().startsWith("parm")) {
                if (propertyValue instanceof IParameterObject) {
                    IParameterObject parameterObject = (IParameterObject) propertyValue;
                    parameterObject.accept(visitor, path.append(new ModelPathComponent(property.getKey(), parameterObject.getType().getClass())));
                } else if(propertyValue instanceof ResourceHolder) {
                    IResource resource = ((ResourceHolder) propertyValue).getResource();
                    resource.accept(visitor, path.append(new ModelPathComponent(property.getKey(), resource.getClass())));
                }
            }
        }
    }

    /**
     * Resets all 'out' and 'in 'properties of this component.
     * To be overwritten to implement special resetting behavior.
     *
     * @see ComposedComponent
     */
    protected void reset() {
        resetInputTransmitters();
        resetInChannels();
        resetOutChannels();
    }

    protected void resetInChannels() {
        for (PacketList channel : inChannels()) {
            channel.clear();
        }
    }

    protected void resetOutChannels() {
        for (PacketList channel : outChannels()) {
            channel.clear();
        }
    }

    private List<PacketList> inChannels() {
        lazyFill(inChannels, "in");
        return inChannels;
    }

    private List<PacketList> outChannels() {
        lazyFill(outChannels, "out");
        return outChannels;
    }

    private void lazyFill(List<PacketList> channels, String prefix) {
        if (!channels.isEmpty()) return;
        Map properties = allCachedComponentProperties();
        for (Object prop : properties.keySet()) {
            if (((String) prop).startsWith(prefix)) {
                Object value = properties.get(prop);
                if (value instanceof PacketList) {
                    channels.add((PacketList) value);
                }
            }
        }
    }

    public List<Class> getMarkerClasses() {
        return markerList;
    }

    protected void publishResults() {
        for (ITransmitter output : allOutputTransmitter) {
            output.transmit();
        }
    }

    /**
     *  This method adds all packet of the source to the inChannel. Overwrite it in order to apply filters
     *  on incoming packets.
     *
     *  @inChannel of this component
     *  @source outChannel of the sending component
     */
    public void filterInChannel(PacketList inChannel, PacketList source) {
        inChannel.addAll(source);
    }

    protected void resetInputTransmitters() {
        transmitCount = 0;
        for (ITransmitter input : allInputTransmitter) {
            input.setTransmitted(false);
        }
    }

    public void notifyTransmitted(ITransmitter transmitter) {
        if (++transmitCount == allInputTransmitter.size()) {
            execute();
        }
    }


    /**
     * Validation of the wiring. As wiring is fix for all periods and
     * iterations it can be done at the beginning of a simulation.
     */
    protected void validateWiring() {
    }

    /**
     * Goal: validation of parameterization changing only between periods
     * and not between iterations.
     */
    public void validateParameterization() {
    }


    protected Map allComponentProperties() {
        Class currentClass = this.getClass();
        Map<String, Object> fields = new HashMap<String, Object>();

        while (currentClass != Component.class) {
            Field[] declaredFields = currentClass.getDeclaredFields();
            for (Field field : declaredFields) {
                String fieldName = field.getName();
                if (fieldName.startsWith("in") || fieldName.startsWith("out") || fieldName.startsWith("parm") || fieldName.startsWith("sub")) {
                    field.setAccessible(true);
                    try {
                        fields.put(fieldName, field.get(this));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            currentClass = currentClass.getSuperclass();
        }
        return fields;
    }

    protected Map propertyCache = null;

    /**
     * Handle with care! Properties are only retrieved once and never change their value!
     * Only useful for props that never change like channels and fixed subcomponents.
     */
    protected Map allCachedComponentProperties() {
        if (propertyCache == null) propertyCache = allComponentProperties();
        return propertyCache;
    }

    public void clearPropertyCache() {
        if (propertyCache != null) {
            propertyCache.clear();
        }
    }

// renamed method from get... to all... as getXY() results in a property xY in the properties map of the component.

    // each access to component.properties calls the method to evaluate the property value

    // todo dk: test case is the only user of this method?
    protected List allParameterizationProperties() {
        List params = new ArrayList();

        Map properties = allComponentProperties();
        for (Object propertyKey : properties.keySet()) {
            if (((String) propertyKey).startsWith("parm")) {
                params.add(properties.get(propertyKey));
            }
        }
        return params;
    }

    /**
     * @return true if a sender is wired to a receiver
     */
    protected boolean isSenderWired(PacketList sender) {
        for (ITransmitter transmitter : getAllOutputTransmitter()) {
            if (transmitter.getSource() == sender) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if a receiver is wired to a sender
     */
    protected boolean isReceiverWired(PacketList receiver) {
        for (ITransmitter transmitter : getAllInputTransmitter()) {
            if (transmitter.getTarget() == receiver) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the number of wired sources
     */
    protected int wiredReceivers(PacketList source) {
        int wiredReceivers = 0;
        for (ITransmitter transmitter : getAllOutputTransmitter()) {
            if (transmitter.getSource() == source) {
                wiredReceivers++;
            }
        }
        return wiredReceivers;
    }

    /**
     * @return true if a sender is wired to exactly one source
     */
    public boolean isOneReceiverWired(PacketList outChannel) {
        return wiredReceivers(outChannel) == 1;
    }

    /**
     * @return true if a sender is wired to at most one source
     */
    public boolean maxOneReceiverWired(PacketList outChannel) {
        return wiredReceivers(outChannel) < 2;
    }

    /**
     * @return the number of wired targets
     */
    protected int wiredSenders(PacketList inChannel) {
        int wiredSenders = 0;
        for (ITransmitter transmitter : getAllInputTransmitter()) {
            if (transmitter.getTarget() == inChannel) {
                wiredSenders++;
            }
        }
        return wiredSenders;
    }

    /**
     * @return true if a target is wired to exactly one sender
     */
    public boolean isOneSenderWired(PacketList inChannel) {
        return wiredSenders(inChannel) == 1;
    }

    /**
     * @return true if a target is wired to at most one sender
     */
    protected boolean maxOneSenderWired(PacketList inChannel) {
        return wiredSenders(inChannel) < 2;
    }

    /**
     * @return true if the component or one of its sub components has at least one parameter
     */
    public boolean hasParameters() {
        Map properties = allComponentProperties();

        for (Object prop : properties.keySet()) {

            String propertyKey = (String) prop;
            if (propertyKey.startsWith("sub")) {

                Component component = (Component) properties.get(prop);
                if (component instanceof DynamicComposedComponent) {
                    component = ((DynamicComposedComponent) component).createDefaultSubComponent();
                }
                boolean result = component.hasParameters();
                if (result) {
                    return true;
                }
            }

            if (propertyKey.startsWith("parm")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasWiredInChannels() {
        return numberOfWiredInChannels() > 0;
    }

    public int numberOfWiredInChannels() {
        int number = 0;
        Map<String, Object> properties = allComponentProperties();
        for (Map.Entry<String, Object> property : properties.entrySet()) {
            if (property.getKey().startsWith("in") && property.getValue() instanceof PacketList && isReceiverWired((PacketList) property.getValue())) {
                number++;
            }
        }
        return number;
    }

    @Override
    public Component clone() throws CloneNotSupportedException {
        return (Component) super.clone();
    }

    /**
     * @return name of the component
     */
    @Override
    public String toString() {
        return name;
    }
}
