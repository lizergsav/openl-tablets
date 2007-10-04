/**
 * MultiLine editor.
 *
 * @author Andrey Naumenko
 */
var MultiLineEditor = Class.create();

MultiLineEditor.prototype = Object.extend(new BaseTextEditor(), {
    eventHandler : null,
    ta : null,

    editor_initialize: function() {
        this.node = document.createElement("div");
        this.ta = document.createElement("textarea");
        this.ta.cols = 30;
        this.ta.rows = 3;
        this.node.appendChild(this.ta);

        this.node.style.position = "absolute";

        var pos = Position.page(this.td);
        pos[1] += Element.Methods.getDimensions(this.td).height;

        this.node.style.left = pos[0] + "px";
        this.node.style.top = pos[1] + "px";
        this.node.zIndex = "10";

        this.eventHandler = this.handleKeyPress.bindAsEventListener(this);
        Event.observe(this.node, "keypress", this.eventHandler);

        this.stopEventPropogation("click");
    },

    show: function(value) {
        this.ta.value = value;
        document.body.appendChild(this.node);
        this.ta.focus();
    },

    handleKeyPress: function (event) {
        switch (event.keyCode) {
            case 13:
                if (!event.ctrlKey) BaseEditor.stopPropagationHandler(event);
            break;
        }
    },

    destroy: function() {
        document.body.removeChild(this.node);
        Event.stopObserving(this.node, "keypress", this.eventHandler);
    },

    /**
     *  Overrides BaseTextEditor.getInputElement
     */
    getInputElement : function() {return this.ta}
});

TableEditor.Editors["multilineText"] = MultiLineEditor;
