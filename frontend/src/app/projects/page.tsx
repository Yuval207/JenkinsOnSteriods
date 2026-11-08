"use client";

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { 
  Plus, 
  FolderGit2, 
  AlertTriangle, 
  GitBranch, 
  X, 
  Loader2, 
  Sparkles, 
  UploadCloud, 
  Archive, 
  FileCode, 
  CheckCircle2, 
  FolderArchive 
} from 'lucide-react';
import Link from 'next/link';
import { formatDistanceToNow } from 'date-fns';
import { useState, useRef } from 'react';
import axios from 'axios';

type Project = {
  id: string;
  name: string;
  repositoryUrl?: string;
  sourceType?: 'GIT' | 'ZIP_UPLOAD';
  originalFileName?: string;
  defaultBranch: string;
  status: string;
  detectedLanguages: string;
  detectedBuildTools: string;
  createdAt: string;
  updatedAt: string;
};

export default function ProjectsPage() {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<'git' | 'zip'>('git');
  
  // Git form state
  const [newProject, setNewProject] = useState({ name: '', repositoryUrl: '', defaultBranch: 'main' });
  
  // ZIP form state
  const [zipFile, setZipFile] = useState<File | null>(null);
  const [zipName, setZipName] = useState('');
  const [zipDescription, setZipDescription] = useState('');
  const [isDragOver, setIsDragOver] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [addError, setAddError] = useState('');

  const { data: projectsData, isLoading, isError } = useQuery({
    queryKey: ['projects'],
    queryFn: async () => {
      const res = await axios.get('/api/v1/projects');
      return res.data.content as Project[];
    }
  });

  const addProjectMutation = useMutation({
    mutationFn: async (project: typeof newProject) => {
      const res = await axios.post('/api/v1/projects', project);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      closeModal();
    },
    onError: (error: any) => {
      setAddError(error.response?.data?.message || 'Failed to add project');
    }
  });

  const uploadZipMutation = useMutation({
    mutationFn: async () => {
      if (!zipFile) throw new Error('Please select a ZIP file');
      const formData = new FormData();
      formData.append('file', zipFile);
      if (zipName.trim()) {
        formData.append('name', zipName.trim());
      }
      if (zipDescription.trim()) {
        formData.append('description', zipDescription.trim());
      }
      setUploadProgress(0);
      const res = await axios.post('/api/v1/projects/upload', formData, {
        timeout: 600000,
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total) {
            const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            setUploadProgress(percent);
          }
        }
      });
      return res.data as Project;
    },
    onSuccess: async (createdProject) => {
      setUploadProgress(null);
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      // Trigger scan in background for the newly uploaded project
      try {
        await axios.post(`/api/v1/projects/${createdProject.id}/scans`, {
          branch: 'main',
          scanners: ['SEMGREP', 'TRIVY', 'GITLEAKS']
        });
      } catch {
        // If scan trigger fails (e.g. docker daemon offline), project is still created
      }
      closeModal();
    },
    onError: (error: any) => {
      setUploadProgress(null);
      setAddError(error.response?.data?.message || error.message || 'Failed to upload ZIP archive');
    }
  });

  const closeModal = () => {
    setIsModalOpen(false);
    setNewProject({ name: '', repositoryUrl: '', defaultBranch: 'main' });
    setZipFile(null);
    setZipName('');
    setZipDescription('');
    setAddError('');
    setIsDragOver(false);
    setUploadProgress(null);
  };

  const handleFileSelect = (file: File) => {
    if (!file.name.toLowerCase().endsWith('.zip')) {
      setAddError('Please select a valid .zip archive');
      return;
    }
    if (file.size > 500 * 1024 * 1024) {
      setAddError('File size exceeds the 500MB limit');
      return;
    }
    setAddError('');
    setZipFile(file);
    if (!zipName) {
      const cleanName = file.name.replace(/\.zip$/i, '');
      setZipName(cleanName);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFileSelect(e.dataTransfer.files[0]);
    }
  };

  const handleAddDemoApp = () => {
    addProjectMutation.mutate({
      name: 'Demo Vulnerable App',
      repositoryUrl: '/Users/yuvalsharma/Desktop/jenkinsOnSteriods/demo-vulnerable-app',
      defaultBranch: 'main'
    });
  };

  const projects = projectsData || [];

  return (
    <div className="space-y-6 pb-12">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-display text-white">Repositories & Projects</h1>
          <p className="text-zinc-400 mt-1 text-sm">Manage source code repositories, upload ZIP packages, and trigger automated scanner pipelines.</p>
        </div>
        <div className="flex items-center gap-2">
          <button 
            className="px-3.5 py-2 text-xs font-semibold rounded-lg bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 flex items-center gap-1.5 transition-all shadow-sm"
            onClick={handleAddDemoApp}
            disabled={addProjectMutation.isPending}
          >
            <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
            Add Demo Vulnerable App
          </button>
          <button className="btn-primary" onClick={() => setIsModalOpen(true)}>
            <Plus className="w-4 h-4" />
            Add New Project
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center items-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-zinc-500" />
        </div>
      ) : isError ? (
        <div className="text-red-400 p-4 rounded bg-red-500/10 border border-red-500/20">
          Failed to load projects. Make sure the backend is running.
        </div>
      ) : projects.length === 0 ? (
        <div className="flex flex-col items-center justify-center p-12 border border-dashed border-white/10 rounded-xl bg-zinc-900/50">
          <FolderGit2 className="w-12 h-12 text-zinc-600 mb-4" />
          <h3 className="text-lg font-medium text-white mb-2">No repositories connected</h3>
          <p className="text-zinc-400 text-center max-w-md mb-6 text-sm">
            Connect a Git repository, upload a project ZIP file, or load the built-in vulnerable demo app to scan immediately.
          </p>
          <div className="flex items-center gap-3">
            <button 
              className="px-4 py-2 text-sm font-semibold rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white flex items-center gap-2 transition-all shadow-lg shadow-indigo-600/20"
              onClick={handleAddDemoApp}
              disabled={addProjectMutation.isPending}
            >
              <Sparkles className="w-4 h-4" />
              Load Demo Vulnerable App
            </button>
            <button className="btn-secondary" onClick={() => setIsModalOpen(true)}>
              <Plus className="w-4 h-4" />
              Add Project / Upload ZIP
            </button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 stagger-children mt-6">
          {projects.map((project) => {
            const isZip = project.sourceType === 'ZIP_UPLOAD';
            return (
              <div key={project.id} className="sg-card-interactive flex flex-col p-5 group relative">
                <Link href={`/projects/${project.id}`} className="absolute inset-0 z-10" />
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className={`w-10 h-10 rounded-lg border flex items-center justify-center shrink-0 ${isZip ? 'bg-purple-500/10 border-purple-500/30' : 'bg-zinc-800 border-white/10'}`}>
                      {isZip ? (
                        <FolderArchive className="w-5 h-5 text-purple-400" />
                      ) : (
                        <GitBranch className="w-5 h-5 text-zinc-300" />
                      )}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="font-semibold text-white text-base leading-tight group-hover:text-indigo-400 transition-colors">
                          {project.name}
                        </h3>
                        <span className={`text-[10px] px-1.5 py-0.5 rounded font-mono font-medium ${isZip ? 'bg-purple-500/15 text-purple-300 border border-purple-500/30' : 'bg-blue-500/15 text-blue-300 border border-blue-500/30'}`}>
                          {isZip ? 'ZIP' : 'GIT'}
                        </span>
                      </div>
                      <div className="text-xs text-zinc-500 font-mono mt-1 flex items-center gap-1.5 truncate max-w-[200px]">
                        {isZip ? (project.originalFileName || 'ZIP Upload') : (project.repositoryUrl || 'Local Repo')}
                      </div>
                    </div>
                  </div>
                </div>
                
                <div className="flex flex-wrap items-center gap-1.5 mb-6 text-[10px] font-mono text-zinc-400">
                  {project.detectedLanguages && project.detectedLanguages.split(',').filter(Boolean).map(lang => (
                    <span key={lang} className="px-1.5 py-0.5 rounded bg-white/5 border border-white/5">{lang.trim()}</span>
                  ))}
                  {project.detectedBuildTools && project.detectedBuildTools.split(',').filter(Boolean).map(tool => (
                    <span key={tool} className="px-1.5 py-0.5 rounded bg-indigo-500/10 text-indigo-300 border border-indigo-500/20">{tool.trim()}</span>
                  ))}
                </div>
                
                <div className="mt-auto pt-4 border-t border-white/5 grid grid-cols-2 gap-4">
                  <div>
                    <span className="text-[10px] text-zinc-500 uppercase tracking-wider block mb-1">Status</span>
                    <div className="flex items-center gap-2">
                      <span className={`text-xs font-semibold ${project.status === 'ACTIVE' ? 'text-emerald-400' : 'text-red-400'}`}>
                        {project.status}
                      </span>
                    </div>
                  </div>
                  
                  <div>
                    <span className="text-[10px] text-zinc-500 uppercase tracking-wider block mb-1">Added</span>
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-medium text-zinc-300">
                        {project.createdAt ? formatDistanceToNow(new Date(project.createdAt), { addSuffix: true }) : 'N/A'}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Add Project Modal with Two Tabs: Git Repo URL & Upload ZIP */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div className="bg-zinc-900 border border-white/10 rounded-xl shadow-2xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between p-4 border-b border-white/10">
              <h2 className="text-lg font-semibold text-white">Add New Project</h2>
              <button 
                onClick={closeModal}
                className="text-zinc-400 hover:text-white transition-colors p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Tab selection */}
            <div className="flex border-b border-white/10 px-5 pt-3 bg-zinc-950/40 gap-2">
              <button
                type="button"
                onClick={() => { setActiveTab('git'); setAddError(''); }}
                className={`pb-2.5 px-3 text-sm font-medium border-b-2 flex items-center gap-2 transition-all ${
                  activeTab === 'git' 
                    ? 'border-indigo-500 text-white font-semibold' 
                    : 'border-transparent text-zinc-400 hover:text-zinc-200'
                }`}
              >
                <GitBranch className="w-4 h-4" />
                Git Repo URL
              </button>
              <button
                type="button"
                onClick={() => { setActiveTab('zip'); setAddError(''); }}
                className={`pb-2.5 px-3 text-sm font-medium border-b-2 flex items-center gap-2 transition-all ${
                  activeTab === 'zip' 
                    ? 'border-indigo-500 text-white font-semibold' 
                    : 'border-transparent text-zinc-400 hover:text-zinc-200'
                }`}
              >
                <Archive className="w-4 h-4" />
                Upload ZIP
              </button>
            </div>
            
            {addError && (
              <div className="mx-5 mt-4 p-3 text-sm text-red-400 bg-red-500/10 border border-red-500/20 rounded-lg flex items-start gap-2">
                <AlertTriangle className="w-4 h-4 mt-0.5 shrink-0" />
                <span>{addError}</span>
              </div>
            )}

            {/* Tab 1: Git Repository */}
            {activeTab === 'git' && (
              <form 
                onSubmit={(e) => {
                  e.preventDefault();
                  addProjectMutation.mutate(newProject);
                }}
                className="p-5 space-y-4"
              >
                {/* Demo Pre-fill */}
                <div className="p-3 bg-indigo-500/10 border border-indigo-500/20 rounded-lg flex items-center justify-between">
                  <div className="text-xs text-indigo-300">
                    <div className="font-semibold">Local Demo Vulnerable App</div>
                    <div className="text-zinc-400 text-[11px]">Pre-seeded with Java, Docker, and secrets</div>
                  </div>
                  <button
                    type="button"
                    onClick={() => setNewProject({
                      name: 'Demo Vulnerable App',
                      repositoryUrl: '/Users/yuvalsharma/Desktop/jenkinsOnSteriods/demo-vulnerable-app',
                      defaultBranch: 'main'
                    })}
                    className="px-2.5 py-1 text-xs font-medium bg-indigo-600 hover:bg-indigo-500 text-white rounded transition-colors"
                  >
                    Fill In
                  </button>
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-zinc-300 mb-1.5">Project Name</label>
                  <input 
                    type="text" 
                    required
                    placeholder="e.g. My Secure App"
                    className="w-full bg-black/50 border border-white/10 rounded-lg px-3 py-2 text-white placeholder:text-zinc-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 text-sm"
                    value={newProject.name}
                    onChange={e => setNewProject({...newProject, name: e.target.value})}
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-zinc-300 mb-1.5">Repository URL or Local Path</label>
                  <input 
                    type="text" 
                    required
                    placeholder="https://github.com/org/repo.git or /path/to/local/repo"
                    className="w-full bg-black/50 border border-white/10 rounded-lg px-3 py-2 text-white placeholder:text-zinc-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 font-mono text-xs"
                    value={newProject.repositoryUrl}
                    onChange={e => setNewProject({...newProject, repositoryUrl: e.target.value})}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-zinc-300 mb-1.5">Default Branch</label>
                  <input 
                    type="text" 
                    required
                    className="w-full bg-black/50 border border-white/10 rounded-lg px-3 py-2 text-white placeholder:text-zinc-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 text-sm"
                    value={newProject.defaultBranch}
                    onChange={e => setNewProject({...newProject, defaultBranch: e.target.value})}
                  />
                </div>
                
                <div className="pt-4 flex items-center justify-end gap-3">
                  <button 
                    type="button" 
                    className="px-4 py-2 text-sm font-medium text-zinc-300 hover:text-white transition-colors"
                    onClick={closeModal}
                  >
                    Cancel
                  </button>
                  <button 
                    type="submit" 
                    disabled={addProjectMutation.isPending}
                    className="btn-primary"
                  >
                    {addProjectMutation.isPending ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        Connecting...
                      </>
                    ) : 'Connect Repository'}
                  </button>
                </div>
              </form>
            )}

            {/* Tab 2: Upload ZIP */}
            {activeTab === 'zip' && (
              <form 
                onSubmit={(e) => {
                  e.preventDefault();
                  uploadZipMutation.mutate();
                }}
                className="p-5 space-y-4"
              >
                {/* Drag and drop zone */}
                <div 
                  onDragOver={handleDragOver}
                  onDragLeave={handleDragLeave}
                  onDrop={handleDrop}
                  onClick={() => fileInputRef.current?.click()}
                  className={`border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition-all flex flex-col items-center justify-center ${
                    isDragOver 
                      ? 'border-indigo-500 bg-indigo-500/10 scale-[0.99]' 
                      : zipFile 
                        ? 'border-emerald-500/50 bg-emerald-500/5 hover:border-emerald-500' 
                        : 'border-white/15 bg-black/30 hover:border-indigo-500/50 hover:bg-white/5'
                  }`}
                >
                  <input 
                    ref={fileInputRef}
                    type="file" 
                    accept=".zip,application/zip,application/x-zip-compressed"
                    className="hidden"
                    onChange={(e) => {
                      if (e.target.files && e.target.files.length > 0) {
                        handleFileSelect(e.target.files[0]);
                      }
                    }}
                  />

                  {zipFile ? (
                    <div className="flex flex-col items-center gap-2">
                      <div className="w-12 h-12 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
                        <CheckCircle2 className="w-6 h-6" />
                      </div>
                      <div className="font-medium text-white text-sm mt-1">{zipFile.name}</div>
                      <div className="text-xs text-zinc-400 font-mono">
                        {(zipFile.size / (1024 * 1024)).toFixed(2)} MB
                      </div>
                      <span className="text-xs text-indigo-400 hover:underline mt-1">Click to replace file</span>
                    </div>
                  ) : (
                    <div className="flex flex-col items-center gap-2">
                      <div className="w-12 h-12 rounded-full bg-zinc-800 border border-white/10 flex items-center justify-center text-zinc-400">
                        <UploadCloud className="w-6 h-6 text-indigo-400" />
                      </div>
                      <div className="font-medium text-white text-sm">
                        Drag & Drop your ZIP here
                      </div>
                      <div className="text-xs text-zinc-400">
                        or click to browse from your computer
                      </div>
                      <div className="text-[11px] text-zinc-500 mt-2 font-mono">
                        Supports Maven, Gradle, Node, Python, C#, Go, Rust • Max 500MB
                      </div>
                    </div>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-zinc-300 mb-1.5">
                    Project Name <span className="text-zinc-500 font-normal">(auto-detected from archive)</span>
                  </label>
                  <input 
                    type="text" 
                    placeholder="e.g. My Application"
                    className="w-full bg-black/50 border border-white/10 rounded-lg px-3 py-2 text-white placeholder:text-zinc-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 text-sm"
                    value={zipName}
                    onChange={e => setZipName(e.target.value)}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-zinc-300 mb-1.5">
                    Description <span className="text-zinc-500 font-normal">(optional)</span>
                  </label>
                  <input 
                    type="text" 
                    placeholder="e.g. Service microservice repository"
                    className="w-full bg-black/50 border border-white/10 rounded-lg px-3 py-2 text-white placeholder:text-zinc-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 text-sm"
                    value={zipDescription}
                    onChange={e => setZipDescription(e.target.value)}
                  />
                </div>

                {uploadProgress !== null && (
                  <div className="space-y-1.5 pt-2">
                    <div className="flex justify-between text-xs text-zinc-400">
                      <span>{uploadProgress < 100 ? `Uploading archive (${uploadProgress}%)...` : 'Processing & extracting files on server...'}</span>
                      <span className="font-mono text-indigo-400">{uploadProgress}%</span>
                    </div>
                    <div className="w-full h-1.5 bg-white/10 rounded-full overflow-hidden">
                      <div 
                        className="h-full bg-gradient-to-r from-indigo-500 to-purple-500 transition-all duration-200"
                        style={{ width: `${uploadProgress}%` }}
                      />
                    </div>
                  </div>
                )}

                <div className="pt-4 flex items-center justify-end gap-3">
                  <button 
                    type="button" 
                    className="px-4 py-2 text-sm font-medium text-zinc-300 hover:text-white transition-colors"
                    onClick={closeModal}
                  >
                    Cancel
                  </button>
                  <button 
                    type="submit" 
                    disabled={!zipFile || uploadZipMutation.isPending}
                    className="btn-primary"
                  >
                    {uploadZipMutation.isPending ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        {uploadProgress !== null && uploadProgress < 100 
                          ? `Uploading (${uploadProgress}%)...` 
                          : 'Extracting & Scanning...'}
                      </>
                    ) : (
                      <>
                        <UploadCloud className="w-4 h-4" />
                        Upload & Scan
                      </>
                    )}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
