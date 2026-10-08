import React, { useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Anchor,
  ArrowLeft,
  ArrowRight,
  Building2,
  Check,
  ChevronRight,
  Clock,
  Download,
  Edit3,
  FileText,
  Layers,
  LogOut,
  MapPin,
  Minus,
  Plus,
  Search,
  ShieldCheck,
  Ship,
  Truck,
  Users,
  X,
  Bell,
  Move,
  MousePointer2,
  Save,
  Trash2,
  ZoomIn,
  ZoomOut,
  RotateCcw,
} from "lucide-react";
import { api, send } from "./api";
import {
  bounds,
  transform,
  expandGroup,
  selection,
  money,
  date,
  statusNames,
} from "./map-utils";
import "./style.css";

const IconButton = ({ title, children, ...props }) => (
  <button className="icon-btn" aria-label={title} title={title} {...props}>
    {children}
  </button>
);
function Field({ label, children, ...props }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children || <input {...props} />}
    </label>
  );
}
function Modal({ title, children, onClose, wide = false }) {
  const ref = useRef();
  useEffect(() => {
    const prior = document.activeElement;
    ref.current.focus();
    const handler = (e) => {
      if (e.key === "Escape") onClose();
      if (e.key === "Tab") {
        const els = [
          ...ref.current.querySelectorAll(
            "button,input,select,textarea,a[href]",
          ),
        ].filter((el) => !el.disabled);
        if (!els.length) return;
        let a = els[0],
          z = els.at(-1);
        if (e.shiftKey && document.activeElement === a) {
          e.preventDefault();
          z.focus();
        } else if (!e.shiftKey && document.activeElement === z) {
          e.preventDefault();
          a.focus();
        }
      }
    };
    document.addEventListener("keydown", handler);
    return () => {
      document.removeEventListener("keydown", handler);
      prior?.focus();
    };
  }, []);
  return (
    <div
      className="modal-bg"
      onMouseDown={(e) => e.target === e.currentTarget && onClose()}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-label={title}
        tabIndex={-1}
        ref={ref}
        className={"modal " + (wide ? "wide" : "")}
      >
        <header>
          <h2>{title}</h2>
          <IconButton title="Fechar" onClick={onClose}>
            <X size={20} />
          </IconButton>
        </header>
        {children}
      </section>
    </div>
  );
}
class Boundary extends React.Component {
  state = { error: false };
  static getDerivedStateFromError() {
    return { error: true };
  }
  render() {
    return this.state.error ? (
      <div className="loading">
        <h2>Não foi possível exibir esta tela.</h2>
        <button onClick={() => location.reload()}>Recarregar</button>
      </div>
    ) : (
      this.props.children
    );
  }
}

function App() {
  const [user, setUser] = useState(null),
    [loading, setLoading] = useState(true),
    [fairs, setFairs] = useState([]),
    [fair, setFair] = useState(null),
    [data, setData] = useState(null),
    [page, setPage] = useState("home"),
    [selected, setSelected] = useState([]),
    [query, setQuery] = useState(""),
    [status, setStatus] = useState("ALL"),
    [modal, setModal] = useState(null),
    [busy, setBusy] = useState(false),
    [toast, setToast] = useState(null),
    [notifications, setNotifications] = useState([]),
    [editMode, setEditMode] = useState(false),
    [multiSelect, setMultiSelect] = useState(false),
    [assembly, setAssembly] = useState(false),
    [quote, setQuote] = useState(null),
    [myBooths, setMyBooths] = useState([]),
    [queues, setQueues] = useState([]),
    [orders, setOrders] = useState([]);
  const navigation = useRef(0);
  const activeUser = useRef(null);
  const admin = user?.role === "ADMIN";
  const currentBooths = data?.booths || [];
  function message(text, type = "success") {
    setToast({ text, type });
  }
  async function run(fn, success) {
    setBusy(true);
    try {
      await fn();
      if (success) message(success);
    } catch (e) {
      message(e.message, "error");
      if (e.status === 401) {
        setUser(null);
        setData(null);
      }
    } finally {
      setBusy(false);
    }
  }
  useEffect(() => {
    api("/auth/me")
      .then(setUser)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);
  useEffect(() => {
    navigation.current++;
    activeUser.current = user?.id ?? null;
    setPage("home");
    setFair(null);
    setData(null);
    setSelected([]);
    setQuote(null);
    setModal(null);
    setMyBooths([]);
    setQueues([]);
    setOrders([]);
    setNotifications([]);
    setEditMode(false);
    if (!user) { setFairs([]); return; }
    const uid = user.id;
    run(async () => {
      const [fs, ns] = await Promise.all([api("/fairs"), api("/notifications")]);
      if (activeUser.current !== uid) return;
      setFairs(fs);
      setNotifications(ns);
    });
  }, [user?.id]);
  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 7000);
    return () => clearTimeout(t);
  }, [toast]);
  useEffect(() => {
    setQuote(null);
  }, [selected, assembly]);
  async function refresh() {
    if (!user) return;
    const epoch = navigation.current;
    const uid = user.id;
    const [fs, ns, next] = await Promise.all([
      api("/fairs"),
      api("/notifications"),
      data ? api("/pavilions/" + data.pavilion.id) : Promise.resolve(null),
    ]);
    if (epoch !== navigation.current || activeUser.current !== uid) return;
    setFairs(fs);
    if (fair) setFair(fs.find((f) => f.id === fair.id));
    if (next) {
      setData(next);
      setSelected((prev) => {
        const nextSelected = prev
          .map((b) => next.booths.find((v) => v.id === b.id))
          .filter(Boolean);
        return JSON.stringify(prev) === JSON.stringify(nextSelected)
          ? prev
          : nextSelected;
      });
    }
    setNotifications(ns);
    if (page === "mine") await loadMine();
  }
  useEffect(() => {
    if (!user) return;
    const t = setInterval(() => {
      if (!busy && !modal && !editMode)
        refresh().catch((e) => {
          if (e.status === 401) {
            setUser(null);
            setData(null);
          } else
            message(
              "Atualização automática indisponível. Tente recarregar a planta.",
              "error",
            );
        });
    }, 20000);
    return () => clearInterval(t);
  }, [user, fair?.id, data?.pavilion.id, page, busy, modal, editMode]);
  async function loadMap(f, p) {
    const epoch = ++navigation.current;
    const uid = user.id;
    setData(null);
    setFair(f);
    setPage("map");
    setSelected([]);
    setQuote(null);
    setEditMode(false);
    setMultiSelect(false);
    const next = await api("/pavilions/" + p.id);
    if (epoch !== navigation.current || activeUser.current !== uid) return null;
    setData(next);
    return next;
  }
  async function loadMine() {
    const uid = user.id;
    const [b, q, o] = await Promise.all([
      api("/me/booths"),
      api("/me/queue"),
      api("/orders"),
    ]);
    if (activeUser.current !== uid) return;
    setMyBooths(b);
    setQueues(q);
    setOrders(o);
  }
  function pick(b, multi = false) {
    const group = expandGroup(b, currentBooths);
    setSelected((prev) =>
      multi
        ? prev.some((p) => p.id === b.id)
          ? prev.filter((p) => !group.some((g) => g.id === p.id))
          : [...prev, ...group.filter((g) => !prev.some((p) => p.id === g.id))]
        : group,
    );
  }
  async function action(path, success) {
    await run(async () => {
      await send(path, selection(selected, assembly));
      await refresh();
    }, success);
  }
  if (loading)
    return (
      <div className="loading">
        <Anchor size={32} />
        <p>Abrindo o portal…</p>
      </div>
    );
  if (!user)
    return <Login onLogin={setUser} run={run} busy={busy} toast={toast} />;
  return (
    <div className="app">
      <aside className="sidebar">
        <a
          className="brand"
          href="#"
          onClick={(e) => {
            e.preventDefault();
            navigation.current++;
            setPage("home");
            setData(null);
            setFair(null);
          }}
        >
          <span className="brand-icon">
            <Anchor />
          </span>
          <span>
            EXPO<span className="brand-light">PORTAL</span>
            <small>Navalshore · NN Logística</small>
          </span>
        </a>
        <div className="workspace-label">ÁREA DO EXPOSITOR</div>
        <button
          className={
            "nav-item " + (page === "home" || page === "map" ? "active" : "")
          }
          onClick={() => {
            navigation.current++;
            setPage("home");
            setFair(null);
            setData(null);
          }}
        >
          <Layers size={19} />
          Feiras e plantas
        </button>
        <button
          className={"nav-item " + (page === "mine" ? "active" : "")}
          onClick={() =>
            run(async () => {
              navigation.current++;
              await loadMine();
              setPage("mine");
            })
          }
        >
          <Building2 size={19} />
          Meus estandes
        </button>
        <button
          className="nav-item"
          onClick={() =>
            run(async () => {
              setNotifications(await api("/notifications"));
              setModal({ type: "notifications" });
              await send("/notifications/read");
            })
          }
        >
          <Bell size={19} />
          Notificações
          {notifications.some((n) => !n.seen) && <i className="dot" />}
        </button>
        {admin && (
          <>
            <div className="workspace-label">ORGANIZAÇÃO</div>
            <button
              className="nav-item"
              onClick={() => setModal({ type: "admin" })}
            >
              <ShieldCheck size={19} />
              Administração
            </button>
          </>
        )}
        <div className="sidebar-foot">
          <span className="avatar">
            {user.contact?.slice(0, 1).toUpperCase()}
          </span>
          <div>
            <strong>{user.contact}</strong>
            <small>{user.company}</small>
            <span className="role">
              {admin ? "Administrador" : "Expositor"}
            </span>
          </div>
          <IconButton
            title="Sair"
            onClick={() =>
              run(async () => {
                await send("/auth/logout");
                setUser(null);
                setData(null);
                setFair(null);
              })
            }
          >
            <LogOut size={17} />
          </IconButton>
        </div>
      </aside>
      <main>
        <header className="topbar">
          <span>
            <span className="muted">Portal do Expositor</span>
            {fair && (
              <>
                {" "}
                <ChevronRight size={14} /> <strong>{fair.name}</strong>
              </>
            )}
          </span>
          <div className="topbar-right">
            <span className="connection">
              <i />
              Conectado
            </span>
            <span className="top-avatar">{user.contact.slice(0, 1)}</span>
          </div>
        </header>
        {page === "home" && (
          <div className="page home">
            <div className="eyebrow">SEU PRÓXIMO ENCONTRO COMEÇA AQUI</div>
            <h1>Encontre o espaço da sua empresa.</h1>
            <p className="lead">
              Explore as plantas, escolha seu estande e acompanhe cada etapa da
              sua participação.
            </p>
            <div className="fair-grid">
              {fairs.map((f) => (
                <article
                  className={
                    "fair-card " +
                    (f.id.startsWith("naval") ? "naval" : "logistica")
                  }
                  key={f.id}
                >
                  <div className="fair-art">
                    <div className="art-grid" />
                    {f.id.startsWith("naval") ? (
                      <Ship size={130} strokeWidth={0.8} />
                    ) : (
                      <Truck size={130} strokeWidth={0.8} />
                    )}
                    <span className="edition">EDIÇÃO 2027</span>
                    <span className="fair-wordmark">
                      {f.id.startsWith("naval") ? (
                        "NAVALSHORE"
                      ) : (
                        <>
                          NN <span>LOGÍSTICA</span>
                        </>
                      )}
                    </span>
                  </div>
                  <div className="fair-card-body">
                    <span className="tag">
                      {f.id.startsWith("naval")
                        ? "INDÚSTRIA NAVAL E OFFSHORE"
                        : "LOGÍSTICA E TRANSPORTE"}
                    </span>
                    <h2>{f.name}</h2>
                    <p>
                      {f.id.startsWith("naval")
                        ? "Conectando a indústria naval. Construindo novas oportunidades."
                        : "O ponto de encontro de quem movimenta a economia."}
                    </p>
                    <div className="fair-meta">
                      <MapPin size={16} />
                      {f.pavilions.length} plantas para explorar{" "}
                      <span>
                        {f.published
                          ? "Orçamentos publicados"
                          : "Condições em configuração"}
                      </span>
                    </div>
                    {f.pavilions.map((p) => (
                      <button
                        className="pavilion-link"
                        key={p.id}
                        onClick={() => run(() => loadMap(f, p))}
                      >
                        <span>
                          <Layers size={17} />
                          {p.name}
                        </span>
                        <ArrowRight size={18} />
                      </button>
                    ))}
                  </div>
                </article>
              ))}
            </div>
            <div className="steps">
              <div>
                <span>01</span>
                <strong>Explore a planta</strong>
                <p>Navegue pelos pavilhões e compare os espaços.</p>
              </div>
              <div>
                <span>02</span>
                <strong>Conheça seu orçamento</strong>
                <p>Confira área, montagem, taxas e condições.</p>
              </div>
              <div>
                <span>03</span>
                <strong>Garanta sua participação</strong>
                <p>Reserve, contrate ou entre na fila de preferência.</p>
              </div>
            </div>
          </div>
        )}
        {page === "map" && data && (
          <div className="map-page">
            <div className="map-heading">
              <div>
                <button
                  className="text-btn"
                  onClick={() => {
                    navigation.current++;
                    setPage("home");
                    setFair(null);
                    setData(null);
                  }}
                >
                  <ArrowLeft size={15} />
                  Todas as feiras
                </button>
                <h1>{fair?.name}</h1>
                <p className="muted">
                  Escolha um estande para ver os detalhes e o orçamento.
                </p>
              </div>
              <div className="heading-actions">
                <a
                  className="button secondary"
                  href={data.pavilion.original_pdf}
                  target="_blank"
                  rel="noreferrer"
                >
                  <FileText size={16} />
                  PDF original
                </a>
                {admin && (
                  <button
                    className={"button " + (editMode ? "primary" : "secondary")}
                    onClick={() => {
                      setEditMode(!editMode);
                      setSelected([]);
                    }}
                  >
                    <Edit3 size={16} />
                    {editMode ? "Concluir edição" : "Editar planta"}
                  </button>
                )}
              </div>
            </div>
            <div className="map-tabs">
              {fair?.pavilions.map((p) => (
                <button
                  className={p.id === data.pavilion.id ? "active" : ""}
                  key={p.id}
                  onClick={() => run(() => loadMap(fair, p))}
                >
                  <Layers size={16} />
                  {p.name}
                </button>
              ))}
              <span>
                {currentBooths.filter((b) => b.status === "AVAILABLE").length}{" "}
                disponíveis · {currentBooths.length} estandes
              </span>
            </div>
            {!data.pavilion.reviewed && (
              <div className="notice">
                <ShieldCheck size={17} />
                Planta em conferência. Explore os espaços; reservas serão
                liberadas após a validação da organização.
                {admin && (
                  <button
                    className="text-btn"
                    onClick={() =>
                      run(async () => {
                        await send(
                          "/admin/pavilions/" + data.pavilion.id + "/review",
                          { reviewed: true },
                          "PUT",
                        );
                        await refresh();
                      }, "Planta conferida e liberada para reservas.")
                    }
                  >
                    Publicar planta conferida
                  </button>
                )}
              </div>
            )}
            <div className="map-layout">
              <section className="map-panel">
                <div className="map-toolbar">
                  <div className="search">
                    <Search size={17} />
                    <input
                      aria-label="Buscar estande ou empresa"
                      placeholder="Buscar estande ou empresa…"
                      value={query}
                      onChange={(e) => setQuery(e.target.value)}
                    />
                  </div>
                  <select
                    aria-label="Filtrar disponibilidade"
                    value={status}
                    onChange={(e) => setStatus(e.target.value)}
                  >
                    <option value="ALL">Todos os status</option>
                    {Object.entries(statusNames).map(([k, v]) => (
                      <option key={k} value={k}>
                        {v}
                      </option>
                    ))}
                  </select>
                  {editMode && (
                    <button
                      className="button small primary"
                      onClick={() => setModal({ type: "editor", booth: null })}
                    >
                      <Plus size={15} />
                      Novo estande
                    </button>
                  )}
                </div>
                <div className="selection-tools">
                  <label className="check">
                    <input
                      type="checkbox"
                      checked={multiSelect}
                      onChange={(e) => setMultiSelect(e.target.checked)}
                    />
                    Selecionar vários estandes
                  </label>
                  {multiSelect && (
                    <span>Toque nos vizinhos para adicionar ou retirar.</span>
                  )}
                </div>
                <MapCanvas
                  data={data}
                  selected={selected}
                  query={query}
                  status={status}
                  onPick={(b, multi) => pick(b, multi || multiSelect)}
                  editMode={editMode}
                  onEdit={(b) => setModal({ type: "editor", booth: b })}
                />
                <div className="map-legend">
                  {Object.entries(statusNames).map(([k, v]) => (
                    <span key={k}>
                      <i className={"status-dot " + k} />
                      {v}
                    </span>
                  ))}
                  <span className="legend-help">
                    Ctrl/⌘ + clique para selecionar vizinhos
                  </span>
                </div>
                {editMode && (
                  <div className="editor-tip">
                    <Move size={16} />
                    Arraste os vértices no editor ou informe a posição e o
                    tamanho. Duplo clique abre a edição.
                    <button
                      className="text-btn"
                      onClick={() => setModal({ type: "annotations" })}
                    >
                      Textos e ruas
                    </button>
                  </div>
                )}
              </section>
              <aside className="detail-panel">
                {!selected.length ? (
                  <div className="empty-selection">
                    <MousePointer2 size={32} />
                    <h3>Seu espaço começa aqui</h3>
                    <p>
                      Clique em um estande na planta ou na lista para conhecer a
                      área, a disponibilidade e as condições.
                    </p>
                    <div className="booth-list">
                      {currentBooths
                        .filter(
                          (b) =>
                            (status === "ALL" || b.status === status) &&
                            (b.code + " " + b.label)
                              .toLowerCase()
                              .includes(query.toLowerCase()),
                        )
                        .map((b) => (
                          <button key={b.id} onClick={() => pick(b)}>
                            <strong>{b.code}</strong>
                            <span>
                              {b.area == null
                                ? "Conferir área"
                                : b.area + " m²"}
                            </span>
                            <i className={"status-dot " + b.status} />
                          </button>
                        ))}
                    </div>
                  </div>
                ) : (
                  <>
                    <div className="detail-eyebrow">
                      ESTANDE SELECIONADO{selected.length > 1 ? "S" : ""}
                      <button
                        className="text-btn"
                        onClick={() => setSelected([])}
                      >
                        Limpar
                      </button>
                    </div>
                    <h2>{selected.map((b) => b.code).join(" + ")}</h2>
                    <div className="detail-tags">
                      {selected.map((b) => (
                        <span key={b.id} className={"status-pill " + b.status}>
                          {b.code} ·{" "}
                          {b.mine ? "Minha reserva" : statusNames[b.status]}
                        </span>
                      ))}
                    </div>
                    <div className="area-card">
                      <span>Área total</span>
                      <strong>
                        {selected.every((b) => b.area != null)
                          ? selected
                              .reduce((n, b) => n + Number(b.area), 0)
                              .toLocaleString("pt-BR")
                          : "A conferir"}{" "}
                        <small>m²</small>
                      </strong>
                    </div>
                    {selected.map((b) => (
                      <div className="booth-data" key={b.id}>
                        <strong>{b.code}</strong>
                        <span>{b.dimensions || "Dimensões na planta"}</span>
                        {b.label && (
                          <p>
                            <Building2 size={14} />
                            {b.label}
                          </p>
                        )}
                        {b.expires_at && (
                          <p>
                            <Clock size={14} />
                            Até {date(b.expires_at)}
                          </p>
                        )}
                        {b.queuePosition > 0 && (
                          <p>Você é o {b.queuePosition}º na fila.</p>
                        )}
                        <small>
                          {b.queueCount}{" "}
                          {b.queueCount === 1
                            ? "empresa na fila"
                            : "empresas na fila"}
                        </small>
                      </div>
                    ))}
                    <label className="check">
                      <input
                        type="checkbox"
                        checked={assembly}
                        onChange={(e) => setAssembly(e.target.checked)}
                      />
                      Incluir montagem no orçamento
                    </label>
                    <button
                      className="button secondary full"
                      disabled={busy}
                      onClick={() =>
                        run(async () =>
                          setQuote(
                            await send("/quote", selection(selected, assembly)),
                          ),
                        )
                      }
                    >
                      <FileText size={16} />
                      Ver orçamento completo
                    </button>
                    {quote && <Quote quote={quote} />}
                    {selected.every((b) => b.status === "AVAILABLE") && (
                      <button
                        className="button primary full"
                        disabled={busy || !data.pavilion.reviewed || !fair?.published}
                        onClick={() => setModal({ type: "reserveConfirm" })}
                      >
                        Reservar{" "}
                        {selected.length > 1 ? "e unir estandes" : "estande"}
                        <ArrowRight size={16} />
                      </button>
                    )}
                    {selected.every(
                      (b) =>
                        b.status === "AVAILABLE" ||
                        (b.mine && b.status === "RESERVED"),
                    ) && (
                      <button
                        className="button contract full"
                        disabled={
                          busy || !data.pavilion.reviewed || !fair?.published
                        }
                        onClick={() =>
                          run(async () => {
                            const q = await send(
                              "/quote",
                              selection(selected, assembly),
                            );
                            setQuote(q);
                            setModal({ type: "contract", quote: q });
                          })
                        }
                      >
                        Contratar agora
                      </button>
                    )}
                    {selected.length > 1 &&
                      selected.some((b) => b.mine && b.status === "RESERVED") &&
                      selected.some((b) => b.status === "AVAILABLE") && (
                        <button
                          className="button secondary full"
                          disabled={busy || !data.pavilion.reviewed || !fair?.published}
                          onClick={() =>
                            action(
                              "/merge",
                              "Estandes unidos na mesma reserva.",
                            )
                          }
                        >
                          Unir à minha reserva
                        </button>
                      )}
                    {selected.every(
                      (b) => b.status === "RESERVED" && (b.mine || admin),
                    ) && (
                      <button
                        className="button danger-outline full"
                        disabled={busy}
                        onClick={() => setModal({ type: "releaseConfirm" })}
                      >
                        Liberar reserva
                      </button>
                    )}
                    {selected.length === 1 &&
                      ["BLOCKED", "RESERVED", "CONTRACTED"].includes(
                        selected[0].status,
                      ) &&
                      !selected[0].mine && (
                        <button
                          className="button primary full"
                          disabled={busy || (!selected[0].queuePosition && (!fair?.published || !data.pavilion.reviewed))}
                          onClick={() =>
                            run(
                              async () => {
                                const b = selected[0];
                                await send(
                                  "/booths/" + b.id + "/queue",
                                  b.queuePosition
                                    ? undefined
                                    : { version: b.version },
                                  b.queuePosition ? "DELETE" : "POST",
                                );
                                await refresh();
                              },
                              selected[0].queuePosition
                                ? "Você saiu da fila."
                                : "Você entrou na fila de preferência.",
                            )
                          }
                        >
                          {selected[0].queuePosition
                            ? "Sair da fila"
                            : "Entrar na fila de preferência"}
                        </button>
                      )}
                    {admin && selected.length === 1 && (
                      <button
                        className="button secondary full"
                        onClick={() =>
                          setModal({ type: "editor", booth: selected[0] })
                        }
                      >
                        <Edit3 size={15} />
                        Editar este estande
                      </button>
                    )}
                    <p className="detail-foot">
                      A contratação só é concluída com orçamento publicado e
                      aceite das condições comerciais.
                    </p>
                  </>
                )}
              </aside>
            </div>
          </div>
        )}
        {page === "mine" && (
          <div className="page">
            <div className="eyebrow">SUA PARTICIPAÇÃO</div>
            <h1>Meus estandes</h1>
            <p className="lead">
              Reservas, preferências e contratações em um só lugar.
            </p>
            <div className="my-grid">
              {myBooths.map((b) => (
                <article className="my-card" key={b.id}>
                  <span className={"status-pill " + b.status}>
                    {statusNames[b.status]}
                  </span>
                  <h2>{b.code}</h2>
                  <p>
                    {b.pavilion_name} · {b.area} m²
                  </p>
                  {b.expires_at && <small>Prazo: {date(b.expires_at)}</small>}
                  <button
                    className="button secondary full"
                    onClick={() =>
                      run(async () => {
                        const f = fairs.find((f) => f.id === b.fair_id),
                          p = f.pavilions.find((p) => p.id === b.pavilion_id);
                        const d = await loadMap(f, p);
                        const found = d?.booths.find((x) => x.id === b.id);
                        if (found) setSelected(expandGroup(found, d.booths));
                      })
                    }
                  >
                    Ver na planta
                    <ArrowRight size={16} />
                  </button>
                </article>
              ))}
            </div>
            {!myBooths.length && (
              <div className="empty-block">
                Você ainda não reservou um estande. Explore as plantas para
                começar.
              </div>
            )}
            <h2 className="section-title">Fila de preferência</h2>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Estande</th>
                    <th>Pavilhão</th>
                    <th>Posição</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {queues.map((q) => (
                    <tr key={q.booth_id}>
                      <td>{q.code}</td>
                      <td>{q.name}</td>
                      <td>{q.position}º</td>
                      <td>
                        <button
                          className="text-btn danger"
                          onClick={() =>
                            run(async () => {
                              await send(
                                "/booths/" + q.booth_id + "/queue",
                                undefined,
                                "DELETE",
                              );
                              await loadMine();
                            }, "Você saiu da fila.")
                          }
                        >
                          Sair da fila
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <h2 className="section-title">
              {admin ? "Contratações registradas" : "Minhas contratações"}
            </h2>
            <OrderTable
              orders={orders}
              onOpen={(id) =>
                run(async () =>
                  setModal({
                    type: "receipt",
                    order: await api("/orders/" + id),
                  }),
                )
              }
            />
          </div>
        )}
      </main>
      {toast && (
        <div role="status" className={"toast " + toast.type}>
          {toast.type === "success" ? <Check size={18} /> : <X size={18} />}
          <span>{toast.text}</span>
          <IconButton title="Fechar mensagem" onClick={() => setToast(null)}>
            <X size={15} />
          </IconButton>
        </div>
      )}
      {busy && <div className="busy-bar" />}
      {modal?.type === "notifications" && (
        <Modal title="Notificações" onClose={() => setModal(null)}>
          {notifications.length ? (
            notifications.map((n) => (
              <div className="notification" key={n.id}>
                <Bell size={16} />
                <div>
                  <p>{n.message}</p>
                  <small>{date(n.created_at)}</small>
                </div>
              </div>
            ))
          ) : (
            <p className="muted">Nenhuma notificação por enquanto.</p>
          )}
        </Modal>
      )}
      {modal?.type === "reserveConfirm" && (
        <Modal title="Confirmar reserva" onClose={() => setModal(null)}>
          <p>
            Reservar {selected.map((b) => b.code).join(" + ")} por até{" "}
            {fair.reserve_hours} horas?
          </p>
          <p className="muted">
            O prazo termina automaticamente. Você poderá contratar com o
            orçamento publicado ou liberar o espaço.
          </p>
          <button
            className="button primary full"
            disabled={busy}
            onClick={() =>
              run(async () => {
                await send("/reserve", selection(selected, assembly));
                setModal(null);
                await refresh();
              }, "Reserva confirmada.")
            }
          >
            Confirmar reserva
          </button>
        </Modal>
      )}
      {modal?.type === "releaseConfirm" && (
        <Modal title="Liberar reserva" onClose={() => setModal(null)}>
          <p>
            Confirma a liberação de {selected.map((b) => b.code).join(" + ")}? A
            primeira empresa da fila receberá a preferência automaticamente.
          </p>
          <button
            className="button danger full"
            disabled={busy}
            onClick={() =>
              run(async () => {
                await send("/release", selection(selected));
                setModal(null);
                setSelected([]);
                await refresh();
              }, "Reserva liberada.")
            }
          >
            Confirmar liberação
          </button>
        </Modal>
      )}
      {modal?.type === "contract" && (
        <ContractModal
          quote={modal.quote}
          busy={busy}
          onClose={() => setModal(null)}
          onConfirm={() =>
            run(async () => {
              const result = await send("/contract", {
                ...selection(selected, assembly),
                quoteHash: modal.quote.hash,
                acceptTerms: true,
              });
              const receipt = await api("/orders/" + result.id);
              await refresh();
              setModal({ type: "receipt", order: receipt });
            }, "Contratação registrada com sucesso.")
          }
        />
      )}
      {modal?.type === "receipt" && (
        <Modal
          title="Comprovante de contratação"
          wide
          onClose={() => setModal(null)}
        >
          <Receipt order={modal.order} />
        </Modal>
      )}
      {modal?.type === "editor" && (
        <BoothEditor
          booth={modal.booth}
          data={data}
          busy={busy}
          onClose={() => setModal(null)}
          onSave={(draft) =>
            run(async () => {
              if (modal.booth)
                await send("/admin/booths/" + modal.booth.id, draft, "PUT");
              else
                await send(
                  "/admin/pavilions/" + data.pavilion.id + "/booths",
                  draft,
                );
              setModal(null);
              await refresh();
            }, "Estande salvo.")
          }
          onDelete={() =>
            run(async () => {
              await send(
                "/admin/booths/" +
                  modal.booth.id +
                  "?version=" +
                  modal.booth.version,
                undefined,
                "DELETE",
              );
              setModal(null);
              setSelected([]);
              await refresh();
            }, "Estande excluído.")
          }
          onAssign={(id) =>
            run(async () => {
              await send("/admin/booths/" + modal.booth.id + "/assign", {
                version: modal.booth.version,
                userId: id,
              });
              setModal(null);
              await refresh();
            }, "Reserva atribuída à empresa.")
          }
        />
      )}
      {modal?.type === "annotations" && (
        <Annotations
          data={data}
          run={run}
          busy={busy}
          onClose={() => setModal(null)}
          refresh={refresh}
        />
      )}
      {modal?.type === "admin" && (
        <Admin
          fairs={fairs}
          busy={busy}
          run={run}
          onClose={() => setModal(null)}
          refresh={refresh}
          onReceipt={(o) => setModal({ type: "receipt", order: o })}
        />
      )}
    </div>
  );
}

function Login({ onLogin, run, busy, toast }) {
  const [register, setRegister] = useState(false);
  const [success, setSuccess] = useState("");
  async function submit(e) {
    e.preventDefault();
    const f = Object.fromEntries(new FormData(e.currentTarget));
    await run(async () => {
      if (register) {
        const result = await send("/auth/register", f);
        setSuccess(result.message);
        setRegister(false);
      } else onLogin(await send("/auth/login", f));
    });
  }
  return (
    <div className="login-page">
      <section className="login-art">
        <div className="login-brand">
          <Anchor size={29} /> EXPOPORTAL
        </div>
        <div>
          <div className="eyebrow">NAVALSHORE · NN LOGÍSTICA</div>
          <h1>
            Grandes conexões.
            <br />O espaço certo
            <br />
            para acontecer.
          </h1>
          <p>
            O portal da sua participação nas feiras que conectam a indústria
            naval e a logística.
          </p>
        </div>
        <div className="login-footer">
          PORTAL DO EXPOSITOR <span>EDIÇÃO 2027</span>
        </div>
        <Ship className="login-ship" size={420} strokeWidth={0.35} />
      </section>
      <section className="login-form">
        <span className="mobile-brand">
          <Anchor />
          EXPOPORTAL
        </span>
        <span className="tag">BEM-VINDO AO PORTAL</span>
        <h2>{register ? "Cadastre sua empresa" : "Acesse sua conta"}</h2>
        <p>
          {register
            ? "O cadastro será aprovado pela organização."
            : "Entre para explorar as plantas e gerenciar sua participação."}
        </p>
        {success && <div className="notice">{success}</div>}
        <form onSubmit={submit}>
          <Field
            label="E-mail"
            type="email"
            name="email"
            autoComplete="username"
            placeholder="voce@empresa.com.br"
            required
            maxLength={254}
          />
          {register && (
            <>
              <Field label="Empresa" name="company" required maxLength={160} />
              <Field label="Seu nome" name="contact" required maxLength={160} />
              <Field label="Telefone" name="phone" type="tel" maxLength={40} />
            </>
          )}
          <Field
            label="Senha"
            type="password"
            name="password"
            autoComplete={register ? "new-password" : "current-password"}
            minLength={register ? 12 : undefined}
            required
            placeholder={register ? "Pelo menos 12 caracteres" : "Sua senha"}
          />
          <button className="button primary full" disabled={busy}>
            {register ? "Solicitar cadastro" : "Entrar no portal"}
            <ArrowRight size={18} />
          </button>
        </form>
        {toast?.type === "error" && (
          <p role="alert" className="form-error">
            {toast.text}
          </p>
        )}
        <button
          className="text-btn login-switch"
          onClick={() => {
            setRegister(!register);
            setSuccess("");
          }}
        >
          {register
            ? "Já tem uma conta? Entrar"
            : "Primeiro acesso? Cadastre sua empresa"}
        </button>
        <p className="login-help">
          Esqueceu a senha? Solicite a redefinição à organização da feira.
        </p>
        <div className="secure-note">
          <ShieldCheck size={16} />
          Acesso protegido para expositores e organização.
        </div>
      </section>
    </div>
  );
}

function MapCanvas({
  data,
  selected,
  query,
  status,
  onPick,
  editMode,
  onEdit,
}) {
  const [zoom, setZoom] = useState(1),
    [original, setOriginal] = useState(false),
    [tool, setTool] = useState("select");
  const stage = useRef();
  const pan = useRef();
  const p = data.pavilion;
  const w = Number(p.map_width),
    h = Number(p.map_height);
  useEffect(() => {
    setZoom(1);
    setOriginal(false);
  }, [p.id]);
  useEffect(() => { if (editMode) setOriginal(false); }, [editMode]);
  const svg = (
    <svg
      viewBox={`0 0 ${w} ${h}`}
      aria-label={"Planta interativa de " + p.name}
      role="group"
    >
      <image
        href={original ? p.image_path : p.clean_path}
        width={w}
        height={h}
        pointerEvents="none"
      />
      {data.booths.map((b) => {
        const sel = selected.some((v) => v.id === b.id),
          filtered =
            (status !== "ALL" && status !== b.status) ||
            !(b.code + " " + b.label)
              .toLowerCase()
              .includes(query.toLowerCase());
        const bb = bounds(b.geometry);
        const font = Number(b.font_size);
        const lines = b.label
          ? b.label.match(/.{1,18}(\s|$)|.{1,18}/g) || [b.label]
          : [];
        const labels = !original;
        return (
          <g
            key={b.id}
            className={"map-booth " + b.status + (sel ? " selected" : "")}
            opacity={filtered ? 0.16 : 1}
            role="button"
            tabIndex={tool === "select" ? 0 : -1}
            aria-label={`Estande ${b.code}, ${b.area ?? "área a conferir"} metros quadrados, ${statusNames[b.status]}`}
            onClick={(e) => {
              if (tool === "select")
                onPick(b, e.ctrlKey || e.metaKey || e.shiftKey);
            }}
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                onPick(b, e.ctrlKey || e.metaKey || e.shiftKey);
              }
            }}
            onDoubleClick={() => editMode && onEdit(b)}
          >
            <title>
              {b.code} · {b.area ?? "?"} m² · {b.label || statusNames[b.status]}
            </title>
            <polygon
              points={b.geometry.map((p) => p.join(",")).join(" ")}
              fill={original ? (sel ? "#25cab8" : "transparent") : undefined}
              fillOpacity={original ? (sel ? 0.32 : 0) : 1}
              stroke={sel ? "#ff6c30" : original ? "transparent" : "#6c808b"}
              strokeWidth={sel ? 2 : 0.45}
              vectorEffect="non-scaling-stroke"
            />
            {labels && (
              <>
                <defs>
                  <clipPath id={"clip-" + b.id}>
                    <polygon
                      points={b.geometry.map((p) => p.join(",")).join(" ")}
                    />
                  </clipPath>
                </defs>
                <g pointerEvents="none" clipPath={"url(#clip-" + b.id + ")"}>
                  <text
                    x={bb.x + bb.w / 2}
                    y={bb.y + bb.h / 2 - lines.length * font * 0.55}
                    textAnchor="middle"
                    dominantBaseline="middle"
                    fontSize={Math.min(7, bb.w * 0.35, bb.h * 0.3)}
                    fontWeight="700"
                    fill="#0b2b42"
                  >
                    {b.code}
                  </text>
                  {lines.map((line, i) => (
                    <text
                      key={i}
                      x={bb.x + bb.w / 2}
                      y={
                        bb.y +
                        bb.h / 2 +
                        (i - (lines.length - 1) / 2) * font * 1.1
                      }
                      textAnchor="middle"
                      dominantBaseline="middle"
                      fontSize={font}
                      fill="#173645"
                    >
                      {line.trim()}
                    </text>
                  ))}
                  <text
                    x={bb.x + bb.w / 2}
                    y={bb.y + bb.h - 2}
                    textAnchor="middle"
                    fontSize={Math.min(5, bb.h * 0.16)}
                    fill="#4c6675"
                  >
                    {b.area ?? "?"} m²
                  </text>
                </g>
              </>
            )}
          </g>
        );
      })}
      {data.annotations.map((a) => (
        <text
          key={a.id}
          x={a.x}
          y={a.y}
          fontSize={a.font_size}
          transform={`rotate(${a.rotation} ${a.x} ${a.y})`}
          fill="#081d31"
          pointerEvents="none"
        >
          {a.text}
        </text>
      ))}
    </svg>
  );
  return (
    <>
      <div className="map-controls">
        <div className="segmented">
          <button
            className={original ? "active" : ""}
            onClick={() => setOriginal(true)}
          >
            PDF original
          </button>
          <button
            className={!original ? "active" : ""}
            onClick={() => setOriginal(false)}
          >
            Situação atual
          </button>
        </div>
        <div className="control-buttons">
          <IconButton
            title="Selecionar estandes"
            onClick={() => setTool("select")}
            className={"icon-btn " + (tool === "select" ? "active" : "")}
          >
            <MousePointer2 size={17} />
          </IconButton>
          <IconButton
            title="Arrastar planta"
            onClick={() => setTool("pan")}
            className={"icon-btn " + (tool === "pan" ? "active" : "")}
          >
            <Move size={17} />
          </IconButton>
          <IconButton
            title="Diminuir zoom"
            onClick={() => setZoom((z) => Math.max(0.7, z / 1.3))}
          >
            <ZoomOut size={17} />
          </IconButton>
          <span>{Math.round(zoom * 100)}%</span>
          <IconButton
            title="Aumentar zoom"
            onClick={() => setZoom((z) => Math.min(8, z * 1.3))}
          >
            <ZoomIn size={17} />
          </IconButton>
          <IconButton
            title="Ajustar planta"
            onClick={() => {
              setZoom(1);
              stage.current.scrollTo(0, 0);
            }}
          >
            <RotateCcw size={17} />
          </IconButton>
        </div>
      </div>
      <div
        className={"map-stage " + (tool === "pan" ? "panning" : "")}
        ref={stage}
        onPointerDown={(e) => {
          if (tool !== "pan") return;
          pan.current = {
            x: e.clientX,
            y: e.clientY,
            left: stage.current.scrollLeft,
            top: stage.current.scrollTop,
          };
          e.currentTarget.setPointerCapture(e.pointerId);
        }}
        onPointerMove={(e) => {
          if (!pan.current) return;
          stage.current.scrollLeft =
            pan.current.left - e.clientX + pan.current.x;
          stage.current.scrollTop = pan.current.top - e.clientY + pan.current.y;
        }}
        onPointerUp={() => (pan.current = null)}
        onPointerCancel={() => (pan.current = null)}
      >
        <div
          className="map-sheet"
          style={{ width: zoom * 100 + "%", minWidth: zoom * 100 + "%" }}
        >
          {svg}
        </div>
      </div>
      {original && (
        <div className="original-caption">
          A imagem preserva as informações impressas no PDF. Use “Situação
          atual” para ver reservas, edições e nomes atualizados.
        </div>
      )}
    </>
  );
}

function Quote({ quote }) {
  return (
    <div className="quote">
      <h3>Orçamento detalhado</h3>
      {!quote.ready && (
        <div className="notice">
          Orçamento preliminar. Preços ou metragens ainda não foram
          publicados/conferidos.
        </div>
      )}
      <dl>
        <div>
          <dt>Área × valor por m²</dt>
          <dd>
            {quote.area} m² × {money(quote.rate)}
          </dd>
        </div>
        <div>
          <dt>Locação do espaço</dt>
          <dd>{money(quote.space)}</dd>
        </div>
        <div>
          <dt>Montagem{quote.assembly ? "" : " (não selecionada)"}</dt>
          <dd>{money(quote.mounting)}</dd>
        </div>
        <div>
          <dt>Taxas fixas</dt>
          <dd>{money(quote.fee)}</dd>
        </div>
        <div>
          <dt>Tributos adicionais ({quote.taxPercent}%)</dt>
          <dd>{money(quote.tax)}</dd>
        </div>
        <div className="quote-total">
          <dt>Total</dt>
          <dd>{money(quote.total)}</dd>
        </div>
      </dl>
    </div>
  );
}
function ContractModal({ quote, busy, onClose, onConfirm }) {
  const [accepted, setAccepted] = useState(false);
  return (
    <Modal title="Confirmar contratação" wide onClose={onClose}>
      <p>
        {quote.fair} · {quote.pavilion}
        <br />
        <strong>{quote.items.map((i) => i.code).join(" + ")}</strong>
      </p>
      <Quote quote={quote} />
      <h3>Condições comerciais</h3>
      <div className="terms">
        {quote.terms || "Condições ainda não publicadas."}
      </div>
      <label className="check">
        <input
          type="checkbox"
          checked={accepted}
          onChange={(e) => setAccepted(e.target.checked)}
        />
        Li o orçamento e aceito as condições comerciais acima em nome da minha
        empresa.
      </label>
      <button
        className="button primary full"
        disabled={!accepted || !quote.ready || busy}
        onClick={onConfirm}
      >
        Confirmar contratação · {money(quote.total)}
      </button>
      <small className="muted">
        A confirmação registra a contratação e o aceite. O pagamento segue as
        condições definidas pela organização.
      </small>
    </Modal>
  );
}
function Receipt({ order }) {
  const q = order.snapshot;
  return (
    <div className="receipt">
      <div className="receipt-head">
        <Anchor size={28} />
        <div>
          <h2>{q.fair}</h2>
          <p>Comprovante de contratação</p>
        </div>
        <span className="status-pill">
          {order.status === "ACTIVE" ? "Registrada" : "Cancelada"}
        </span>
      </div>
      <dl>
        <div>
          <dt>Número</dt>
          <dd>{order.id}</dd>
        </div>
        <div>
          <dt>Empresa</dt>
          <dd>{q.company}</dd>
        </div>
        <div>
          <dt>Estandes</dt>
          <dd>{q.items.map((i) => i.code).join(" + ")}</dd>
        </div>
        <div>
          <dt>Pavilhão</dt>
          <dd>{q.pavilion}</dd>
        </div>
        <div>
          <dt>Aceite registrado</dt>
          <dd>{date(order.accepted_at)}</dd>
        </div>
      </dl>
      <Quote quote={q} />
      <h3>Condições aceitas</h3>
      <div className="terms">{q.terms}</div>
      {order.cancellation_reason && (
        <p>Cancelamento: {order.cancellation_reason}</p>
      )}
      <button
        className="button secondary full no-print"
        onClick={() => window.print()}
      >
        <Download size={16} />
        Imprimir / salvar em PDF
      </button>
    </div>
  );
}
function OrderTable({ orders, onOpen }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Número</th>
            <th>Data</th>
            <th>Total</th>
            <th>Situação</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {orders.map((o) => (
            <tr key={o.id}>
              <td>
                {o.id.slice(0, 8)}
                {o.company && <small>{o.company}</small>}
              </td>
              <td>{date(o.accepted_at)}</td>
              <td>{money(o.total)}</td>
              <td>{o.status === "ACTIVE" ? "Registrada" : "Cancelada"}</td>
              <td>
                <button className="text-btn" onClick={() => onOpen(o.id)}>
                  Comprovante
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {!orders.length && (
        <p className="empty-block">Nenhuma contratação registrada.</p>
      )}
    </div>
  );
}

function BoothEditor({
  booth,
  data,
  busy,
  onClose,
  onSave,
  onDelete,
  onAssign,
}) {
  const p = data.pavilion;
  const [draft, setDraft] = useState(() =>
    booth
      ? {
          version: booth.version,
          code: booth.code,
          area: booth.area ?? "",
          dimensions: booth.dimensions,
          geometry: booth.geometry,
          fontSize: booth.font_size,
          label: booth.label,
          status: booth.status,
          verified: booth.verified,
        }
      : {
          version: 0,
          code: "",
          area: "",
          dimensions: "",
          geometry: [
            [460, 320],
            [500, 320],
            [500, 350],
            [460, 350],
          ],
          fontSize: 6,
          label: "",
          status: "AVAILABLE",
          verified: false,
        },
  );
  const [users, setUsers] = useState([]),
    [assigned, setAssigned] = useState(""),
    [deleteConfirm, setDeleteConfirm] = useState(false),
    [raw, setRaw] = useState(JSON.stringify(draft.geometry)),
    [geoError, setGeoError] = useState("");
  const svgRef = useRef(),
    drag = useRef();
  const box = bounds(draft.geometry);
  useEffect(() => {
    if (booth)
      api("/admin/users")
        .then(setUsers)
        .catch(() => {});
  }, []);
  function change(k, v) {
    setDraft((d) => ({ ...d, [k]: v }));
  }
  function geometry(points) {
    setDraft((d) => ({ ...d, geometry: points }));
    setRaw(JSON.stringify(points));
    setGeoError("");
  }
  function move(e) {
    if (drag.current == null) return;
    const pt = svgRef.current.createSVGPoint();
    pt.x = e.clientX;
    pt.y = e.clientY;
    const pos = pt.matrixTransform(svgRef.current.getScreenCTM().inverse());
    const points = draft.geometry.map((p, i) =>
      i === drag.current
        ? [
            +Math.max(
              0,
              Math.min(Number(data.pavilion.map_width), pos.x),
            ).toFixed(3),
            +Math.max(
              0,
              Math.min(Number(data.pavilion.map_height), pos.y),
            ).toFixed(3),
          ]
        : p,
    );
    geometry(points);
  }
  function submit(e) {
    e.preventDefault();
    if (geoError) return;
    onSave({
      ...draft,
      area: draft.area === "" ? null : Number(draft.area),
      fontSize: Number(draft.fontSize),
    });
  }
  const view = bounds(draft.geometry),
    margin = Math.max(view.w, view.h) * 0.5 + 15;
  return (
    <Modal
      title={booth ? "Editar estande " + booth.code : "Adicionar estande"}
      wide
      onClose={onClose}
    >
      <form onSubmit={submit}>
        <div className="editor-layout">
          <div>
            <div className="form-grid">
              <Field
                label="Código"
                required
                maxLength={40}
                value={draft.code}
                onChange={(e) => change("code", e.target.value)}
              />
              <Field
                label="Área comercial (m²)"
                type="number"
                min="0.01"
                step="0.01"
                value={draft.area}
                onChange={(e) => change("area", e.target.value)}
              />
            </div>
            <Field
              label="Dimensões físicas / observações"
              maxLength={160}
              value={draft.dimensions}
              onChange={(e) => change("dimensions", e.target.value)}
            />
            <Field
              label="Nome exibido na planta"
              maxLength={160}
              value={draft.label}
              onChange={(e) => change("label", e.target.value)}
            />
            <div className="form-grid">
              <Field
                label="Tamanho da fonte"
                type="number"
                min="2"
                max="30"
                step="any"
                value={draft.fontSize}
                onChange={(e) => change("fontSize", e.target.value)}
              />
              <Field label="Ocupação">
                <select
                  value={draft.status}
                  disabled={!["AVAILABLE", "BLOCKED"].includes(draft.status)}
                  onChange={(e) => change("status", e.target.value)}
                >
                  {Object.entries(statusNames).map(([k, v]) => (
                    <option key={k} value={k}>
                      {v}
                    </option>
                  ))}
                </select>
              </Field>
            </div>
            <label className="check">
              <input
                type="checkbox"
                checked={draft.verified}
                onChange={(e) => change("verified", e.target.checked)}
              />
              Conferi código, metragem e contorno com o PDF
            </label>
            <h3>Posição e tamanho na planta</h3>
            <p className="muted">
              As unidades abaixo definem o desenho; a área comercial é informada
              separadamente.
            </p>
            <div className="form-grid">
              {[
                ["x", "Posição X"],
                ["y", "Posição Y"],
                ["w", "Largura visual"],
                ["h", "Altura visual"],
              ].map(([k, label]) => (
                <Field
                  key={k}
                  label={label}
                  type="number"
                  step="any"
                  min={k === "w" || k === "h" ? 2 : 0}
                  value={+box[k].toFixed(2)}
                  onChange={(e) => {
                    const v = Number(e.target.value);
                    if (
                      Number.isFinite(v) &&
                      (k === "w" || k === "h" ? v >= 2 : v >= 0)
                    )
                      geometry(transform(draft.geometry, { ...box, [k]: v }));
                  }}
                />
              ))}
            </div>
            <Field label="Vértices do polígono (X, Y)">
              <textarea
                rows={4}
                value={raw}
                onChange={(e) => {
                  setRaw(e.target.value);
                  try {
                    const pts = JSON.parse(e.target.value);
                    if (
                      !Array.isArray(pts) ||
                      pts.length < 3 ||
                      pts.some(
                        (p) =>
                          !Array.isArray(p) ||
                          p.length !== 2 ||
                          p.some((x) => !Number.isFinite(x)),
                      )
                    )
                      throw Error();
                    change("geometry", pts);
                    setGeoError("");
                  } catch {
                    setGeoError(
                      "Use uma lista com pelo menos 3 pares numéricos, como [[0,0],[20,0],[20,20],[0,20]].",
                    );
                  }
                }}
              />
            </Field>
            {geoError && <p className="form-error">{geoError}</p>}
          </div>
          <div className="editor-preview">
            <span className="tag">PRÉVIA AO VIVO</span>
            <svg
              ref={svgRef}
              viewBox={`${view.x - margin} ${view.y - margin} ${view.w + margin * 2} ${view.h + margin * 2}`}
              onPointerMove={move}
              onPointerUp={() => (drag.current = null)}
              onPointerCancel={() => (drag.current = null)}
            >
              <image
                href={p.clean_path}
                width={p.map_width}
                height={p.map_height}
              />
              <polygon
                points={draft.geometry.map((p) => p.join(",")).join(" ")}
                fill="#d4f6ed"
                stroke="#0ba592"
                strokeWidth=".7"
              />
              <defs>
                <clipPath id="preview-clip">
                  <polygon
                    points={draft.geometry.map((p) => p.join(",")).join(" ")}
                  />
                </clipPath>
              </defs>
              <g clipPath="url(#preview-clip)">
                <text
                  x={box.x + box.w / 2}
                  y={box.y + box.h / 2 - 7}
                  textAnchor="middle"
                  fontSize="6"
                  fill="#0b2b42"
                >
                  {draft.code}
                </text>
                <text
                  x={box.x + box.w / 2}
                  y={box.y + box.h / 2 + 2}
                  textAnchor="middle"
                  fontSize={draft.fontSize}
                  fill="#0b2b42"
                >
                  {draft.label}
                </text>
              </g>
              {draft.geometry.map((p, i) => (
                <circle
                  key={i}
                  cx={p[0]}
                  cy={p[1]}
                  r={Math.max(view.w, view.h) / 45}
                  fill="#ff6c30"
                  className="vertex"
                  onPointerDown={(e) => {
                    drag.current = i;
                    e.currentTarget.setPointerCapture(e.pointerId);
                  }}
                />
              ))}
            </svg>
            <p>
              Arraste os pontos laranja para ajustar o contorno. Alterações de
              fonte aparecem nesta prévia e na “Situação atual”.
            </p>
            {booth?.source_label && (
              <div className="notice">
                Ocupação no PDF: {booth.source_label}
              </div>
            )}
          </div>
        </div>
        <div className="modal-actions">
          <button className="button primary" disabled={busy || !!geoError}>
            <Save size={16} />
            Salvar alterações
          </button>
          <button type="button" className="button secondary" onClick={onClose}>
            Cancelar
          </button>
          {booth && (
            <button
              type="button"
              className="button danger-outline"
              onClick={() => setDeleteConfirm(true)}
            >
              <Trash2 size={16} />
              Excluir
            </button>
          )}
        </div>
      </form>
      {deleteConfirm && (
        <div className="notice">
          Excluir este estande da planta atual?{" "}
          <button
            className="text-btn danger"
            disabled={busy}
            onClick={onDelete}
          >
            Confirmar exclusão
          </button>
          <button className="text-btn" onClick={() => setDeleteConfirm(false)}>
            Manter
          </button>
        </div>
      )}
      {booth && ["AVAILABLE", "BLOCKED"].includes(booth.status) && (
        <div className="assign">
          <h3>Atribuir reserva a uma empresa</h3>
          <p className="muted">
            Vincule uma ocupação existente a um cadastro aprovado.
          </p>
          <select
            aria-label="Empresa titular"
            value={assigned}
            onChange={(e) => setAssigned(e.target.value)}
          >
            <option value="">Escolha uma empresa</option>
            {users
              .filter((u) => u.enabled)
              .map((u) => (
                <option key={u.id} value={u.id}>
                  {u.company} · {u.email}
                </option>
              ))}
          </select>
          <button
            className="button secondary"
            disabled={!assigned || busy}
            onClick={() => onAssign(assigned)}
          >
            Atribuir reserva
          </button>
        </div>
      )}
    </Modal>
  );
}

function Admin({ fairs, busy, run, onClose, refresh, onReceipt }) {
  const [tab, setTab] = useState("pricing"),
    [users, setUsers] = useState([]),
    [audit, setAudit] = useState([]),
    [orders, setOrders] = useState([]),
    [reset, setReset] = useState(null),
    [password, setPassword] = useState(""),
    [cancel, setCancel] = useState(null),
    [reason, setReason] = useState("");
  useEffect(() => {
    run(async () => {
      const [u, a, o] = await Promise.all([
        api("/admin/users"),
        api("/admin/audit"),
        api("/orders"),
      ]);
      setUsers(u);
      setAudit(a);
      setOrders(o);
    });
  }, []);
  async function saveUser(u, changes) {
    await send(
      "/admin/users/" + u.id,
      { enabled: u.enabled, role: u.role, ...changes },
      "PUT",
    );
    setUsers(await api("/admin/users"));
  }
  return (
    <Modal title="Administração" wide onClose={onClose}>
      <div className="admin-tabs">
        {[
          ["pricing", "Condições comerciais"],
          ["users", "Usuários"],
          ["orders", "Contratações"],
          ["audit", "Histórico"],
        ].map(([key, text]) => (
          <button
            className={tab === key ? "active" : ""}
            key={key}
            onClick={() => setTab(key)}
          >
            {text}
          </button>
        ))}
      </div>
      {tab === "pricing" &&
        fairs.map((f) => (
          <Pricing
            key={f.id + "-" + f.version}
            fair={f}
            busy={busy}
            onSave={(p) =>
              run(async () => {
                await send("/admin/fairs/" + f.id, p, "PUT");
                await refresh();
              }, "Condições comerciais salvas.")
            }
          />
        ))}
      {tab === "users" && (
        <>
          <p className="notice">
            Novos cadastros aguardam aprovação. Redefinir senha, desativar ou
            alterar perfil encerra as sessões desse usuário.
          </p>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Empresa / contato</th>
                  <th>Acesso</th>
                  <th>Perfil</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td>
                      <strong>{u.company}</strong>
                      <small>
                        {u.contact}
                        <br />
                        {u.email}
                        <br />
                        {u.phone}
                      </small>
                    </td>
                    <td>
                      <button
                        className="text-btn"
                        disabled={busy}
                        onClick={() =>
                          run(
                            () => saveUser(u, { enabled: !u.enabled }),
                            u.enabled
                              ? "Acesso desativado."
                              : "Cadastro aprovado.",
                          )
                        }
                      >
                        {u.enabled ? "Desativar" : "Aprovar"}
                      </button>
                    </td>
                    <td>
                      <select
                        aria-label={"Perfil de " + u.email}
                        value={u.role}
                        disabled={busy}
                        onChange={(e) =>
                          run(
                            () => saveUser(u, { role: e.target.value }),
                            "Perfil atualizado.",
                          )
                        }
                      >
                        <option value="EXHIBITOR">Expositor</option>
                        <option value="ADMIN">Administrador</option>
                      </select>
                    </td>
                    <td>
                      <button
                        className="text-btn"
                        onClick={() => {
                          setReset(u);
                          setPassword("");
                        }}
                      >
                        Redefinir senha
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {reset && (
            <div className="notice column">
              <strong>Nova senha para {reset.email}</strong>
              <input
                aria-label="Nova senha"
                type="password"
                minLength={12}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <button
                className="button secondary"
                disabled={busy || password.length < 12}
                onClick={() =>
                  run(async () => {
                    await saveUser(reset, { password });
                    setReset(null);
                    setPassword("");
                  }, "Senha redefinida.")
                }
              >
                Salvar nova senha
              </button>
            </div>
          )}
        </>
      )}
      {tab === "orders" && (
        <>
          <OrderTable
            orders={orders}
            onOpen={(id) =>
              run(async () => onReceipt(await api("/orders/" + id)))
            }
          />
          <h3>Cancelar contratação</h3>
          <p className="muted">
            O cancelamento libera os espaços para suas filas e mantém o
            histórico do aceite.
          </p>
          <select
            aria-label="Contratação para cancelar"
            value={cancel || ""}
            onChange={(e) => setCancel(e.target.value)}
          >
            <option value="">Selecione uma contratação</option>
            {orders
              .filter((o) => o.status === "ACTIVE")
              .map((o) => (
                <option key={o.id} value={o.id}>
                  {o.company} · {o.id.slice(0, 8)} · {money(o.total)}
                </option>
              ))}
          </select>
          <Field label="Motivo do cancelamento">
            <textarea
              maxLength={500}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              rows={3}
            />
          </Field>
          <button
            className="button danger"
            disabled={!cancel || reason.trim().length < 8 || busy}
            onClick={() =>
              run(async () => {
                await send("/admin/orders/" + cancel + "/cancel", { reason });
                setOrders(await api("/orders"));
                setCancel(null);
                setReason("");
                await refresh();
              }, "Contratação cancelada e fila processada.")
            }
          >
            Confirmar cancelamento
          </button>
        </>
      )}
      {tab === "audit" && (
        <>
          <div className="notice">
            Últimas 500 operações. Para o conjunto completo, faça o download do
            arquivo JSON.
          </div>
          <a className="button secondary" href="/api/admin/export" download>
            <Download size={16} />
            Exportar dados e histórico
          </a>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Responsável</th>
                  <th>Ação</th>
                  <th>Detalhes</th>
                </tr>
              </thead>
              <tbody>
                {audit.map((a) => (
                  <tr key={a.id}>
                    <td>{date(a.created_at)}</td>
                    <td>{a.actor}</td>
                    <td>{a.action}</td>
                    <td>
                      <details>
                        <summary>Ver</summary>
                        <pre>{a.details}</pre>
                      </details>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </Modal>
  );
}
function Pricing({ fair, busy, onSave }) {
  const [p, setP] = useState({
    version: fair.version,
    published: fair.published,
    rate: fair.rate,
    assemblyRate: fair.assembly_rate,
    fixedFee: fair.fixed_fee,
    taxPercent: fair.tax_percent,
    reserveHours: fair.reserve_hours,
    offerHours: fair.offer_hours,
    terms: fair.terms,
  });
  return (
    <form
      className="pricing-form"
      onSubmit={(e) => {
        e.preventDefault();
        onSave(p);
      }}
    >
      <h3>{fair.name}</h3>
      <div className="form-grid">
        {[
          ["rate", "Preço do espaço / m² (R$)"],
          ["assemblyRate", "Montagem / m² (R$)"],
          ["fixedFee", "Taxas fixas / contratação (R$)"],
          ["taxPercent", "Tributos adicionais (%)"],
          ["reserveHours", "Prazo de reserva (horas)"],
          ["offerHours", "Preferência da fila (horas)"],
        ].map(([k, label]) => (
          <Field
            key={k}
            label={label}
            type="number"
            step={k.endsWith("Hours") ? "1" : "0.01"}
            min={k.endsWith("Hours") ? "1" : "0"}
            required
            value={p[k]}
            onChange={(e) => setP({ ...p, [k]: Number(e.target.value) })}
          />
        ))}
      </div>
      <Field label="Condições comerciais, pagamento, itens incluídos, cancelamento e validade">
        <textarea
          rows={6}
          maxLength={20000}
          value={p.terms}
          onChange={(e) => setP({ ...p, terms: e.target.value })}
        />
      </Field>
      <label className="check">
        <input
          type="checkbox"
          checked={p.published}
          onChange={(e) => setP({ ...p, published: e.target.checked })}
        />
        Publicar preços e abrir comercialização
      </label>
      <button className="button primary" disabled={busy}>
        Salvar condições
      </button>
    </form>
  );
}
function Annotations({ data, run, busy, onClose, refresh }) {
  const [a, setA] = useState({
      text: "",
      x: 500,
      y: 250,
      fontSize: 10,
      rotation: 0,
    }),
    [id, setId] = useState(null);
  return (
    <Modal title="Textos e ruas da planta" onClose={onClose}>
      <p className="muted">
        Adicione ou edite textos sobre a planta. Os textos originais do PDF
        permanecem na imagem de referência.
      </p>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          run(async () => {
            await send(
              "/admin/pavilions/" +
                data.pavilion.id +
                "/annotations" +
                (id ? "/" + id : ""),
              a,
              id ? "PUT" : "POST",
            );
            await refresh();
            onClose();
          }, "Texto salvo.");
        }}
      >
        <Field
          label="Texto"
          required
          maxLength={160}
          value={a.text}
          onChange={(e) => setA({ ...a, text: e.target.value })}
        />
        <div className="form-grid">
          {[
            ["x", "X"],
            ["y", "Y"],
            ["fontSize", "Fonte"],
            ["rotation", "Rotação (graus)"],
          ].map(([key, label]) => (
            <Field
              key={key}
              label={label}
              type="number"
              step="0.1"
              value={a[key]}
              onChange={(e) => setA({ ...a, [key]: Number(e.target.value) })}
            />
          ))}
        </div>
        <button className="button primary" disabled={busy}>
          Salvar texto
        </button>
      </form>
      {data.annotations.map((v) => (
        <div className="annotation-row" key={v.id}>
          <span>{v.text}</span>
          <button
            className="text-btn"
            onClick={() => {
              setId(v.id);
              setA({
                text: v.text,
                x: Number(v.x),
                y: Number(v.y),
                fontSize: Number(v.font_size),
                rotation: Number(v.rotation),
                version: v.version,
              });
            }}
          >
            Editar
          </button>
          <button
            className="text-btn danger"
            disabled={busy}
            onClick={() =>
              run(async () => {
                await send(
                  "/admin/annotations/" + v.id + "?version=" + v.version,
                  undefined,
                  "DELETE",
                );
                await refresh();
                onClose();
              }, "Texto excluído.")
            }
          >
            Excluir
          </button>
        </div>
      ))}
    </Modal>
  );
}
createRoot(document.getElementById("root")).render(
  <Boundary>
    <App />
  </Boundary>,
);
