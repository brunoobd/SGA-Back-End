package com.backend.sga.rest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.sga.model.Ambiente;
import com.backend.sga.model.Aula;
import com.backend.sga.model.Ausencia;
import com.backend.sga.model.Competencia;
import com.backend.sga.model.DevolveDisp;
import com.backend.sga.model.Erro;
import com.backend.sga.model.Periodo;
import com.backend.sga.model.Professor;
import com.backend.sga.model.RecebeBuscaAmbiente;
import com.backend.sga.model.Sucesso;
import com.backend.sga.repository.AulaRepository;
import com.backend.sga.repository.CompetenciaRepository;
import com.backend.sga.repository.ProfessorRepository;

@CrossOrigin
@RestController
@RequestMapping("/api/professor")
public class ProfessorRestController {

	@Autowired
	private ProfessorRepository professorRepository;

	@Autowired
	private CompetenciaRepository competenciaRepository;

	@Autowired
	private AulaRepository aulaRepository;

	@RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> criarProf(@RequestBody Professor prof) {
		if (prof != null) {
			professorRepository.save(prof);

			List<Competencia> un = prof.getCompetencia();
			for (int i = 0; i < un.size(); i++) {
				Competencia competencia = new Competencia();
				// setando os valores da unidade para competencia
				competencia.setUnidadeCurricular(un.get(i).getUnidadeCurricular());
				// setando o nivel
				competencia.setNivel(un.get(i).getNivel());
				// trazendo os atributos dos professore
				competencia.setProfessor(prof);
				competenciaRepository.save(competencia);
			}

			professorRepository.save(prof);
			Sucesso sucesso = new Sucesso(HttpStatus.OK, "Sucesso");
			Object[] filtro = new Object[2];
			filtro[0] = sucesso;
			filtro[1] = prof.getId();

			ResponseEntity<Object> okpost = new ResponseEntity<Object>(filtro, HttpStatus.OK);
			return okpost;
		} else {
			Erro erro = new Erro(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possivel cadastrar o Professor", null);
			return new ResponseEntity<Object>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/desativar/{id}", method = RequestMethod.PUT)
	public ResponseEntity<Object> desativarProf(@PathVariable("id") Long id, Professor prof,
			HttpServletRequest request) {		
		prof = professorRepository.findById(id).get();
		prof.setAtivo(false);
		professorRepository.save(prof);
		Sucesso sucesso = new Sucesso(HttpStatus.OK, "Sucesso");
		return new ResponseEntity<Object>(sucesso, HttpStatus.OK);
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	public Iterable<Professor> listaDnl() {
		return professorRepository.findAll();
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Object> atualizarProf(@PathVariable("id") Long id, @RequestBody Professor prof,
			HttpServletRequest request) {
		if (prof.getId() != id) {
			Erro erro = new Erro(HttpStatus.INTERNAL_SERVER_ERROR, "ID inválido", null);
			return new ResponseEntity<Object>(erro, HttpStatus.INTERNAL_SERVER_ERROR);
		} else {
			professorRepository.save(prof);
			Sucesso sucesso = new Sucesso(HttpStatus.OK, "Sucesso");
			return new ResponseEntity<Object>(sucesso, HttpStatus.OK);
		}
	}

	// metodo para trazer os professores digitados (Pedido Kalebe)
	@RequestMapping(value = "/buscapalavra/{nome}", method = RequestMethod.GET)
	public Iterable<Professor> buscaPalavraChave(@PathVariable("nome") String nome) {
		// retorna o like da query que eles irão escrever
		return professorRepository.palavraChave(nome);
	}

	// retorna apenas o prof, ambiente e se esta em aula
	@RequestMapping(value = "/professorDisp/", method = RequestMethod.GET)
	public ArrayList<DevolveDisp> buscarDisponibilidadeProfessor() {
		List<Professor> listaProf = (List<Professor>) professorRepository.findAll();
		int hora = LocalTime.now().getHour();
		Calendar data = Calendar.getInstance();
		Ambiente ambiente = null;
		Periodo periodo = null;
		boolean emAula;

		if (hora < 12) {
			periodo = Periodo.MANHA;
		} else if (hora > 12 && hora < 18) {
			periodo = Periodo.TARDE;
		} else if (hora >= 18) {
			periodo = Periodo.NOITE;
		}

		ArrayList<DevolveDisp> listaDisp = new ArrayList<DevolveDisp>();

		for (int i = 0; i < listaProf.size(); i++) {

			DevolveDisp devolveDisp = new DevolveDisp();
			devolveDisp.setProfessor(listaProf.get(i));

			List<Aula> listaAula = aulaRepository.buscaProf(listaProf.get(i), data, periodo);
			if (listaAula.isEmpty()) {
				devolveDisp.setEmAula(false);
				devolveDisp.setAmbiente(null);
			} else {
				devolveDisp.setEmAula(true);
				devolveDisp.setAmbiente(listaAula.get(i).getAmbiente());
			}
			
			listaDisp.add(devolveDisp);
		}
		return listaDisp;
	}
	
	@RequestMapping(value = "/disponibilidade/periodo", method = RequestMethod.GET)
	public ArrayList<Aula> disponivelDataPeriodo(@RequestBody RecebeBuscaAmbiente busca){
		Calendar data = busca.getDataInicio();
		boolean dia[] = busca.getDiasSemana();
		int diaSemana = data.get(Calendar.DAY_OF_WEEK);
		
		ArrayList<Aula> aulas = new ArrayList<Aula>();
		
		
		while(data.before(busca.getDataFinal()) || data.equals(busca.getDataFinal())) {
			if(dia[diaSemana - 1] == true) {
				Optional<Aula> ocupado = aulaRepository.ocupadoProfessor(data, busca.getPeriodo(), busca.getProfessor());
				
				if(!ocupado.isEmpty()) {
					aulas.add(ocupado.get());
				}
			}
			data.add(Calendar.DAY_OF_MONTH, 1);
		}
		
		
		
		return aulas;
	}
	
	@RequestMapping(value = "/disponibilidade", method = RequestMethod.GET)
	public List<Professor> disponibilidadeProf(@RequestBody RecebeBuscaAmbiente busca){
		
		ArrayList<Professor> ocupados = new ArrayList<Professor>();
		List<Professor> profsDb = (List<Professor>) professorRepository.findAll();
		
		Calendar data = busca.getDataInicio();
		int diaSemana = data.get(Calendar.DAY_OF_WEEK);
		boolean dia[] = busca.getDiasSemana();
		Calendar dataFinal = busca.getDataFinal();
		
		while(data.before(dataFinal) || data.equals(dataFinal)) {
			
			if(dia[diaSemana - 1] == true) {
				Optional<Professor> ocupado = aulaRepository.disponibilidade(busca.getUnidadeCurricular(), busca.getPeriodo(), data);
				
				if(!ocupado.isEmpty()) {
					ocupados.add(ocupado.get());
				}
			}
			
			data.add(Calendar.DAY_OF_MONTH, 1);
		}
		
		for(int i = 0; i < profsDb.size(); i++) {
			for(int j = 0; j < ocupados.size(); j++) {
				if(profsDb.get(i) == ocupados.get(j)) {
					profsDb.remove(i);
				}
			}
		}

		return profsDb;
	}
	
	@RequestMapping(value = "/disponibilidadeProf/periodo", method = RequestMethod.POST)
	public ArrayList<Aula> disponivelPeriodoProf(@RequestBody RecebeBuscaAmbiente busca){
		Calendar data = busca.getDataInicio();
		boolean dia[] = busca.getDiasSemana();
		int diaSemana = data.get(Calendar.DAY_OF_WEEK);
		
		ArrayList<Aula> aulas = new ArrayList<Aula>();
		
		while(data.before(busca.getDataFinal()) || data.equals(busca.getDataFinal())) {
			if(dia[diaSemana - 1] == true) {
				//Optional<Aula> ocupado = aulaRepository.ocupadoPorDataPeriodo(data, busca.getPeriodo(), busca.getAmbiente());
				Optional<Aula> ocupado = aulaRepository.ocupadoPorDataPeriodoProf(data, busca.getPeriodo(), busca.getProfessor());
				
				if(!ocupado.isEmpty()) {
					aulas.add(ocupado.get());
				}
			}
			data.add(Calendar.DAY_OF_MONTH, 1);
		}
		
		return aulas;
	}
	
	
	@RequestMapping(value = "/orderProf", method = RequestMethod.GET)
	public List<Professor> orderProfessor(){
		return professorRepository.orderProf();
	}
	
	
	@RequestMapping(value = "/buscProf", method = RequestMethod.GET)
	public List<Professor> ordernarProCrEUc(@RequestParam("nomeCr") String nomeCr, @RequestParam("nomeUc") String nomeUc){
		return professorRepository.listProfcuc(nomeCr, nomeUc);
	}
	
	
	//METODO DASHBOARD (COLUNA)
    @RequestMapping(value = "/diaria", method = RequestMethod.GET)
    public double[] busca (@RequestParam("id") Long id, @RequestParam("data_inicio") String data_inicio, @RequestParam("data_final") String data_final, Professor prof){
        
        //formatando o formato da Data
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        
        //transcrevendo para Calendar
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(sdf.parse(data_inicio));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        Calendar calendar2 = Calendar.getInstance();
        try {
            calendar2.setTime(sdf.parse(data_final));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        List<Aula> lista = aulaRepository.buscaTempo(id, calendar, calendar2);
        
        double diaria = 0;

       //percorrer o for para que recebemos o valor
        for (int l = 0; l < lista.size(); l++) {
            diaria += lista.get(l).getCargaDiaria();
            System.out.println(diaria);
        }
        
        Optional<Professor> profe = professorRepository.findById(id);
        
        double[] horas = new double[2];
        horas[0] = diaria;
        horas[1] = profe.get().getCargaSemanal();
        
        return horas;
        
    }
    
    @RequestMapping(value = "/emAula", method = RequestMethod.GET)
	public ArrayList<Object> retornaEmAula() {
		
    	List<Professor> prof = (List<Professor>) professorRepository.findAll();
		int hora = LocalTime.now().getHour();
		Calendar data = Calendar.getInstance();
		Periodo periodo = null;
		boolean emAula;

		if (hora < 12) {
			periodo = Periodo.MANHA;
		} else if (hora > 12 && hora < 18) {
			periodo = Periodo.TARDE;
		} else if (hora >= 18) {
			periodo = Periodo.NOITE;
		}

		ArrayList<Object> valor = new ArrayList<Object>();
		
		for (int i = 0; i < prof.size(); i++) {
			if (aulaRepository.buscaProf(prof.get(i), data, periodo).isEmpty()) {
				emAula = false;
			} else {
				emAula = true;
			}			
			
			Object result[] = new Object[2];
			result[0] = prof.get(i);
			result[1] = emAula;
			
			valor.add(result);
		}

		return valor;
	}
	
	

}
