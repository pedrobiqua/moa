package moa;

import java.util.LinkedList;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.lazy.neighboursearch.KDTreeSimple;
import moa.streams.generators.AssetNegotiationGenerator;

// PARA RODAR ESSE MONSTRO, BASTA COMPILAR E USAR:
// java -cp moa/target/test-classes:moa/target/classes moa.TestKdTree > output.txt
public class TestKdTree {
    public static void main(String[] args) {
        try {
            // CRIO UMA STREAM QUALQUER
            AssetNegotiationGenerator stream = new AssetNegotiationGenerator();
            stream.prepareForUse();

            int numDim = stream.nextInstance().instance.numValues();
            KDTreeSimple kdtree = new KDTreeSimple(numDim);
            InstancesHeader header = stream.getHeader();
            Instances dataset = new Instances(header);

            int count = 0;
            int total = 500;

            // FAÇO A EXECUÇÃO 500 VEZES E VERIFICO SE ESTÁ BALANCEADA
            while (stream.hasMoreInstances() && count < total) {
                Instance inst = stream.nextInstance().getData();
                dataset.add(inst);
                kdtree.update(inst);
                // System.out.println(inst + " " + kdtree.isBalanced());
                count++;
            }

            kdtree.printInOrder();

            // EXEMPLO DE KDTREE BALANCEADA
            KDTreeSimple kdtree_balanced = new KDTreeSimple(numDim);
            kdtree_balanced.buildTree(dataset);
            System.out.println(kdtree_balanced.isBalanced()); // ESTÁ BALANCEADA, SÓ VER A IMAGEM
            kdtree_balanced.printInOrder();

            // TESTE COM JANELA
            stream.prepareForUse();
            KDTreeSimple kdtree_window = new KDTreeSimple(numDim);
            int window_size = 500;
            total = 1000;
            count = 0;

            LinkedList<Instance> window = new LinkedList<Instance>();
            while (stream.hasMoreInstances() && count < total) {
                Instance inst = stream.nextInstance().getData();
                if (count < window_size) {
                    kdtree_window.update(inst);
                    window.addLast(inst);
                } else {
                    // remover a primeira instancia!
                    Instance removed_instance = window.removeFirst();
                    kdtree_window.remove(removed_instance);
                    kdtree_window.update(inst);
                    window.addLast(inst);
                    kdtree_window.printInOrder();
                    System.out.println();

                }
                // System.out.println(inst + " " + kdtree_window.isBalanced());
                count++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
