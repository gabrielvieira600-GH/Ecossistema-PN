import React from 'react';
import { ArrowRight, CalendarDays, CheckCircle2, Globe2, MapPin, Ship, Users, Building2, TrendingUp, Layers } from 'lucide-react';
import nnLogo from './assets/nn.png';
import river from './assets/nn-river.jpg';
import './nn-landing.css';

export default function NNLanding({ fair, onExpose, onOpenPavilion }) {
  const pavilions = fair?.pavilions || [];
  const primary = pavilions.find(p => /vit[oó]ria|r[eé]gia|pavilh[aã]o 1/i.test(p.name)) || pavilions[0];
  return <div className="nn-land">
    <section className="nn-hero">
      <div className="nn-hero-photo" style={{ backgroundImage: `linear-gradient(90deg,#092334 2%,rgba(9,35,52,.72) 35%,rgba(9,35,52,.22)),url(${river})` }} aria-hidden="true" />
      <div className="nn-hero-inner">
        <span className="nn-overline"><span className="nn-pulse"/> 5ª EDIÇÃO · MANAUS, AMAZONAS</span>
        <h1>O futuro da navegação<br/><em>acontece aqui.</em></h1>
        <p>Conecte sua empresa aos protagonistas da indústria naval fluvial, portos, transporte e logística da Amazônia.</p>
        <div className="nn-event-info"><span><CalendarDays size={18}/> 6, 7 e 8 de abril de 2027</span><span><MapPin size={18}/> Manaus · AM</span></div>
        <div className="nn-actions"><button className="nn-primary" onClick={() => primary && onExpose(primary)} disabled={!primary}>Quero expor <ArrowRight size={19}/></button><a className="nn-secondary" href="#nn-about">Conhecer o evento <ArrowRight size={16}/></a></div>
      </div>
      <div className="nn-hero-logo"><img src={nnLogo} alt="NN Logística"/></div>
    </section>
    <section className="nn-stats" aria-label="Resultados da edição de 2026"><div><strong>9 mil+</strong><span>visitantes com poder de decisão</span></div><div><strong>100+</strong><span>expositores</span></div><div><strong>370+</strong><span>marcas representadas</span></div><div><strong>R$ 520 mi</strong><span>em acordos comerciais reportados</span></div></section>
    <section className="nn-section nn-about" id="nn-about"><div className="nn-section-head"><span className="nn-overline">SOBRE A NN LOGÍSTICA</span><h2>Onde a indústria encontra<br/>novas oportunidades.</h2></div><div className="nn-about-copy"><p>A NN Logística reúne empresas, autoridades e especialistas para impulsionar negócios, inovação e conhecimento nos setores da navegação e da logística amazônica.</p><p>Um ambiente voltado ao relacionamento com tomadores de decisão, novos parceiros comerciais e à apresentação de soluções para um mercado estratégico.</p><a href="https://nnlogistica.com.br/" target="_blank" rel="noopener noreferrer">Conheça o site oficial <ArrowRight size={17}/></a></div></section>
    <section className="nn-section nn-sectors"><span className="nn-overline">QUEM PARTICIPA</span><h2>Os principais atores do setor,<br/>em um só lugar.</h2><div className="nn-sector-grid">{[
      ['Armadores e navegação','Operadores fluviais e companhias de transporte',Ship],
      ['Estaleiros e construção naval','Indústria, equipamentos e navipeças',Building2],
      ['Portos e terminais','Infraestrutura e soluções portuárias',Layers],
      ['Logística multimodal','Transporte, distribuição e operações',Globe2],
      ['Investidores e negócios','Empresas e representantes comerciais',TrendingUp],
      ['Autoridades e especialistas','Instituições, reguladores e profissionais',Users],
    ].map(([name,desc,Icon])=><div className="nn-sector" key={name}><span className="nn-sector-icon"><Icon size={23}/></span><h3>{name}</h3><p>{desc}</p></div>)}</div></section>
    <section className="nn-section nn-exhibit"><div><span className="nn-overline">SUA MARCA EM EVIDÊNCIA</span><h2>Seu próximo negócio<br/>começa no estande.</h2><p>Explore a planta interativa, confira áreas e localizações, simule valores, selecione montagem e envie sua solicitação diretamente ao time comercial.</p><div className="nn-feature-list"><span><CheckCircle2 size={17}/> Planta interativa dos dois pavilhões</span><span><CheckCircle2 size={17}/> Orçamento e opções de montagem</span><span><CheckCircle2 size={17}/> Solicitação comercial pelo portal</span></div><button className="nn-primary" onClick={()=>primary && onExpose(primary)} disabled={!primary}>Quero expor <ArrowRight size={19}/></button></div><div className="nn-pavilions"><strong>Explore os pavilhões</strong>{pavilions.map(p=><button key={p.id} onClick={()=>onOpenPavilion(p)}><Layers size={20}/><span>{p.name}</span><ArrowRight size={18}/></button>)}</div></section>
    <footer className="nn-footer"><img src={nnLogo} alt="NN Logística"/><span>6 a 8 de abril de 2027 · Manaus, AM</span><a href="https://nnlogistica.com.br/" target="_blank" rel="noopener noreferrer">nnlogistica.com.br</a></footer>
  </div>;
}
