/*
 * The MIT License
 *
 * Copyright (c) 2018, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.pipeline.modeldefinition.generator;

import hudson.Extension;
import hudson.model.Descriptor;
import org.jenkinsci.plugins.pipeline.modeldefinition.when.DeclarativeStageConditional;
import org.jenkinsci.plugins.pipeline.modeldefinition.when.DeclarativeStageConditionalDescriptor;
import org.jenkinsci.plugins.structs.describable.DescribableModel;
import org.jenkinsci.plugins.structs.describable.UninstantiatedDescribable;
import org.jenkinsci.plugins.workflow.cps.Snippetizer;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class WhenDirective extends AbstractDirective<WhenDirective> {
    private DeclarativeStageConditional conditional;
    private boolean beforeAgent;

    @DataBoundConstructor
    public WhenDirective(DeclarativeStageConditional conditional, boolean beforeAgent) {
        this.conditional = conditional;
        this.beforeAgent = beforeAgent;
    }

    public DeclarativeStageConditional getConditional() {
        return conditional;
    }

    public boolean isBeforeAgent() {
        return beforeAgent;
    }

    @Extension
    public static class DescriptorImpl extends DirectiveDescriptor<WhenDirective> {
        @Override
        @Nonnull
        public String getName() {
            return "when";
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "When Condition";
        }

        @Override
        @Nonnull
        public List<Descriptor> getDescriptors() {
            // For some reason, returning forGenerator directly won't cast from DeclarativeStageConditionalDescriptor to Descriptor. Fun.
            return new ArrayList<>(DeclarativeStageConditionalDescriptor.forGenerator());
        }

        @Override
        @Nonnull
        public String toGroovy(@Nonnull WhenDirective directive) {
            if (directive.conditional != null) {
                UninstantiatedDescribable ud = UninstantiatedDescribable.from(directive.conditional);
                DescribableModel model = ud.getModel();
                if (model != null) {
                    StringBuilder result = new StringBuilder();
                    result.append("when {\n");

                    result.append(conditionalToGroovy(directive.conditional));
                    if (directive.isBeforeAgent()) {
                        result.append("beforeAgent true\n");
                    }
                    result.append("}\n");
                    return result.toString();
                }
            }

            return "// No valid when condition defined\n";
        }


        @Nonnull
        private String conditionalToGroovy(@Nonnull DeclarativeStageConditional<?> conditional) {
            DeclarativeStageConditionalDescriptor descriptor = conditional.getDescriptor();

            if (descriptor.getAllowedChildrenCount() == 0) {
                return Snippetizer.object2Groovy(UninstantiatedDescribable.from(conditional)) + "\n";
            } else {
                StringBuilder result = new StringBuilder();
                result.append(descriptor.getName()).append(" {\n");

                for (DeclarativeStageConditional c : conditional.getChildren()) {
                    result.append(conditionalToGroovy(c));
                }
                result.append("}\n");
                return result.toString();
            }
        }
    }
}