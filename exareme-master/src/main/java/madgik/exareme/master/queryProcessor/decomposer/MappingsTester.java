package madgik.exareme.master.queryProcessor.decomposer;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import it.unibz.krdb.obda.model.Function;
import it.unibz.krdb.obda.model.OBDAMappingAxiom;
import it.unibz.krdb.obda.model.OBDASQLQuery;
import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.model.Term;
import it.unibz.krdb.obda.model.impl.FunctionalTermImpl;
import it.unibz.krdb.obda.model.impl.SQLQueryImpl;
import it.unibz.krdb.obda.model.impl.VariableImpl;
import it.unibz.krdb.obda.r2rml.R2RMLReader;
import madgik.exareme.master.queryProcessor.decomposer.dag.NodeHashValues;
import madgik.exareme.master.queryProcessor.decomposer.query.NonUnaryWhereCondition;
import madgik.exareme.master.queryProcessor.decomposer.query.Operand;
import madgik.exareme.master.queryProcessor.decomposer.query.SQLQuery;
import madgik.exareme.master.queryProcessor.decomposer.query.SQLQueryParser;
import madgik.exareme.master.queryProcessor.decomposer.query.UnaryWhereCondition;
import madgik.exareme.master.queryProcessor.decomposer.util.InterfaceAdapter;
import madgik.exareme.master.queryProcessor.decomposer.util.Util;
import madgik.exareme.master.queryProcessor.estimator.NodeSelectivityEstimator;

public class MappingsTester {

	public static void main(String[] args) {
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = null;
		try {
			//ontology = manager.loadOntologyFromOntologyDocument(new File("/home/dimitris/ontopv1/iswc2014-benchmark/LUBM/univ-benchQL.owl"));
			ontology = manager.loadOntologyFromOntologyDocument(new File("/home/dimitris/ontopv1/npd-benchmark/ontology/npd-v2-ql.owl"));
		} catch (OWLOntologyCreationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Set<String> domains=new HashSet<String>();
		Set<String> ranges=new HashSet<String>();
		for(OWLObjectProperty op:ontology.getObjectPropertiesInSignature()){
			for(OWLObjectPropertyDomainAxiom domain:ontology.getObjectPropertyDomainAxioms(op)){
				if(!domain.getClassesInSignature().isEmpty()){
					domains.add(op.getIRI().toString());
					break;
				}
			}
			for(OWLObjectPropertyRangeAxiom range:ontology.getObjectPropertyRangeAxioms(op)){
				if(!range.getClassesInSignature().isEmpty()){
					ranges.add(op.getIRI().toString());
					break;
				}
			}
		}
		for(OWLDataProperty op:ontology.getDataPropertiesInSignature()){
			for(OWLDataPropertyDomainAxiom domain:ontology.getDataPropertyDomainAxioms(op)){
				if(!domain.getClassesInSignature().isEmpty()){
					domains.add(op.getIRI().toString());
					break;
				}
			}
		}
		
		//String obdafile="/home/dimitris/ontopv1/iswc2014-benchmark/LUBM/univ-benchQL.ttl";
		String obdafile="/home/dimitris/ontopv1/npdnew/npd-benchmark/mappings/postgres/ontop>=1.17/npd-v2-ql-postgres-ontop1.17.ttl";
		R2RMLReader reader=null;
		try {
			reader = new R2RMLReader(obdafile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 List<OBDAMappingAxiom> ax=reader.readMappings();
		 Map<String, Set<Set<String>>> queryOutputs=new HashMap<String, Set<Set<String>>>();
		 for(OBDAMappingAxiom axiom:ax){
			 String s=null;
			 OBDASQLQuery sql=axiom.getSourceQuery();
			 if(sql instanceof SQLQueryImpl){
				 SQLQueryImpl sqlImpl=(SQLQueryImpl) sql;
				 s=sqlImpl.getSQLQuery();				 
			 }
			 for(Function f:axiom.getTargetQuery()){
				 Predicate a=f.getFunctionSymbol();
				 if(domains.contains(a.getName())){
					Term t=f.getTerm(0); 
					Set<String> outputs=new HashSet<String>();
					if(t instanceof FunctionalTermImpl){
						FunctionalTermImpl fti=(FunctionalTermImpl)t;
						for(int i=1;i<fti.getTerms().size();i++){
							Term term=fti.getTerm(i);
							if(term instanceof VariableImpl){
								VariableImpl v=(VariableImpl)term;
								outputs.add(v.getName());
							}
						}
					}
					if(queryOutputs.containsKey(s)){
						queryOutputs.get(s).add(outputs);
					}
					else{
						Set<Set<String>> outputsForquery=new HashSet<Set<String>>();
						outputsForquery.add(outputs);
						queryOutputs.put(s, outputsForquery);
					}
					
				 }
				 
				 if(ranges.contains(a.getName())){
						Term t=f.getTerm(1); 
						Set<String> outputs=new HashSet<String>();
						if(t instanceof FunctionalTermImpl){
							FunctionalTermImpl fti=(FunctionalTermImpl)t;
							for(int i=1;i<fti.getTerms().size();i++){
								Term term=fti.getTerm(i);
								if(term instanceof VariableImpl){
									VariableImpl v=(VariableImpl)term;
									outputs.add(v.getName());
								}
							}
						}
						if(queryOutputs.containsKey(s)){
							queryOutputs.get(s).add(outputs);
						}
						else{
							Set<Set<String>> outputsForquery=new HashSet<Set<String>>();
							outputsForquery.add(outputs);
							queryOutputs.put(s, outputsForquery);
						}
						
					 }
				 
			 }
		 }
		 NodeSelectivityEstimator nse = null;
		 Map<String, Set<ViewInfo>> viewinfos=new HashMap<String, Set<ViewInfo>>(); 
		 for(String sql:queryOutputs.keySet()){
			 NodeHashValues hashes=new NodeHashValues();
				
				
				try {
					nse = new NodeSelectivityEstimator("/media/dimitris/T/exaremenpd100new2/" + "histograms.json");
				} catch (Exception e) {
					
				}
				hashes.setSelectivityEstimator(nse);
				try{
					SQLQuery query = SQLQueryParser.parse(sql, hashes);
					if(query.getInputTables().size()!=1||!query.getGroupBy().isEmpty()||!query.getOrderBy().isEmpty()){
						continue;
					}
					String table=query.getInputTables().get(0).getName().toLowerCase();
					
					for(Set<String> outputs:queryOutputs.get(sql)){
						double dupl=nse.getDuplicateEstimation(table, outputs);
						if(dupl>1.2){
							String viewName=table+outputs.iterator().next()+Util.createUniqueId();
							String colame=outputs.iterator().next();
							System.out.println("from sql:"+sql);
							System.out.println("table:"+table+" columns:"+outputs+" dupl. est."+dupl);
							System.out.println("view sql: create table "+ viewName
									+ " as select distinct "+colame+" from "+table+" "+query.getWhereSQL());
							
							Set<ViewInfo> viewsForTable=null;
							if(viewinfos.containsKey(table)){
								viewsForTable=viewinfos.get(table);
							}
							else{
								viewsForTable=new HashSet<ViewInfo>();
								viewinfos.put(table, viewsForTable);		
							}
							ViewInfo vi=new ViewInfo(viewName, colame);
							viewsForTable.add(vi);
							for(NonUnaryWhereCondition o:query.getBinaryWhereConditions()){
								vi.addCondition(o);
							}
							for(UnaryWhereCondition o:query.getUnaryWhereConditions()){
								vi.addCondition(o);
							}
						}
						
						Gson gson = new GsonBuilder().registerTypeAdapter(Operand.class, new InterfaceAdapter<Operand>())
			                    .create();
						java.lang.reflect.Type viewType=new TypeToken<Map<String, Set<ViewInfo>>>() {}.getType(); 
				        String jsonStr = gson.toJson(viewinfos, viewType);

				        PrintWriter writer = new PrintWriter("/media/dimitris/T/exaremenpd100new2/" + "views.json", "UTF-8");
				        writer.println(jsonStr);
				        writer.close();
					}
					
				}
				catch(Exception e){
					System.out.println(e.getMessage());
					e.printStackTrace(System.out);
				}
		 }
		 
		 System.out.println("sss");

	}

}
