import React from 'react';
import {ArrowRight, Anchor, Building2, CheckCircle2, Compass, Globe2, Handshake, Layers3, MapPin, Ship, Wrench, Users, ExternalLink} from 'lucide-react';
import navalLogo from './assets/navalshore.jpg';
import './naval-landing.css';

const sectors = [
  {title:'Construção e reparação naval', description:'Estaleiros, modernização de embarcações, projetos e serviços especializados.',Icon:Wrench},
  {title:'Navegação e apoio marítimo', description:'Armadores, operadores e soluções para o transporte e operações marítimas.',Icon:Ship},
  {title:'Offshore e energia', description:'Fornecedores de tecnologia, equipamentos e serviços para operações offshore.',Icon:Compass},
  {title:'Portos e cadeia de fornecedores', description:'Conexão de compradores, fabricantes, distribuidores e prestadores de serviços.',Icon:Building2},
];

export default function NavalLanding({fair,onExpose,onOpenPavilion}) {
  const pavilions = fair?.pavilions || [];
  const primary = pavilions.find(p => /1[º°o]?\s*andar|primeiro/i.test(p.name)) || pavilions[0];
  return <div className="naval-land">
    <section className="naval-hero">
      <div className="naval-orb naval-orb-one" aria-hidden="true"/><div className="naval-orb naval-orb-two" aria-hidden="true"/>
      <div className="naval-hero-content">
        <span className="naval-kicker"><span/> NAVALSHORE · FEIRA E CONFERÊNCIA DA INDÚSTRIA MARÍTIMA</span>
        <h1>O futuro da indústria marítima <em>se conecta aqui.</em></h1>
        <p>Amplie sua presença no setor naval e offshore. Encontre parceiros, apresente soluções e transforme conexões em oportunidades de negócios.</p>
        <div className="naval-meta"><span><MapPin size={18}/> Rio de Janeiro · Brasil</span><span><Anchor size={18}/> Estandes da edição 2027</span></div>
        <div className="naval-hero-actions"><button className="naval-primary" onClick={()=>primary&&onExpose(primary)} disabled={!primary}>Quero expor <ArrowRight size={19}/></button><a className="naval-outline" href="#naval-why">Conheça a Navalshore <ArrowRight size={17}/></a></div>
      </div>
      <div className="naval-hero-visual" aria-label="Identidade Navalshore">
        <div className="naval-hero-radar"/><Ship size={190} strokeWidth={.9} aria-hidden="true"/><div className="naval-logo-glass"><img src={navalLogo} alt="Navalshore" /></div>
        <span className="naval-hero-visual-caption">CONEXÕES · TECNOLOGIA · NEGÓCIOS</span>
      </div>
    </section>
    <section className="naval-facts" aria-label="Destaques do evento"><div><strong>Indústria marítima</strong><span>Ecossistema completo em um só evento</span></div><div><strong>Rodadas de negócios</strong><span>Encontros entre empresas e compradores</span></div><div><strong>Conferências</strong><span>Conhecimento e tendências do setor</span></div><div><strong>Rio de Janeiro</strong><span>Encontro de referência do mercado naval</span></div></section>
    <section className="naval-about naval-section" id="naval-why"><div><span className="naval-eyebrow">SOBRE O EVENTO</span><h2>O ponto de encontro de quem move a indústria marítima.</h2></div><div><p>A Navalshore reúne empresas da construção e reparação naval, navegação, offshore, portos e serviços correlatos em um ambiente dedicado ao relacionamento comercial e à apresentação de soluções.</p><p>Além da exposição, a programação oficial reúne conteúdo técnico e oportunidades de conexão entre fornecedores, compradores e instituições do setor.</p><a href="https://www.navalshore.com.br/" target="_blank" rel="noopener noreferrer">Conheça o site oficial <ExternalLink size={16}/></a></div></section>
    <section className="naval-section naval-sector-section"><span className="naval-eyebrow">SETORES PARTICIPANTES</span><h2>As conexões certas para novos negócios.</h2><div className="naval-sector-grid">{sectors.map(({title,description,Icon})=><article key={title} className="naval-sector"><span><Icon size={25}/></span><h3>{title}</h3><p>{description}</p></article>)}</div></section>
    <section className="naval-section naval-exhibit"><div className="naval-exhibit-text"><span className="naval-eyebrow">SEU LUGAR NA NAVALSHORE 2027</span><h2>Dê visibilidade à sua empresa. Encontre seu estande.</h2><p>Escolha o espaço na planta interativa, compare as metragens, simule seu investimento e envie seu pedido de contratação para análise comercial.</p><div className="naval-benefits"><span><CheckCircle2 size={18}/> Visualização dos dois andares</span><span><CheckCircle2 size={18}/> Orçamento personalizado</span><span><CheckCircle2 size={18}/> Pedido digital de contratação</span></div><button className="naval-primary" onClick={()=>primary&&onExpose(primary)} disabled={!primary}>Quero expor <ArrowRight size={19}/></button></div><aside className="naval-pavilions"><span className="naval-eyebrow">ESCOLHA SEU ESPAÇO</span><h3>Explore as plantas</h3>{pavilions.map(p=><button key={p.id} onClick={()=>onOpenPavilion(p)}><Layers3 size={20}/><span>{p.name}</span><ArrowRight size={18}/></button>)}<small>A disponibilidade está sujeita à atualização da planta e às condições comerciais vigentes.</small></aside></section>
    <footer className="naval-footer"><div><img src={navalLogo} alt="Navalshore"/><span>Feira e Conferência da Indústria Marítima</span></div><a href="https://www.navalshore.com.br/" target="_blank" rel="noopener noreferrer">navalshore.com.br <ExternalLink size={15}/></a></footer>
  </div>;
}
